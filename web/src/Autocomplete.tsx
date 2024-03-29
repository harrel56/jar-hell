import {useAutocomplete, UseAutocompleteReturnValue} from '@mui/base/useAutocomplete/useAutocomplete'
import {useEffect, useState} from 'react'
import {useDebounce} from 'use-debounce'
import {useNavigate} from 'react-router-dom'
import {useFetch} from './hooks/useFetch.ts'

interface Artifact {
  g: string
  a: string
  latestVersion: string
}

interface ListboxProps {
  loading: boolean
  ac: UseAutocompleteReturnValue<Artifact>
}

const toArtifactString = (artifact: Artifact) => `${artifact.g}:${artifact.a}`
const toShortArtifactString = (artifact: Artifact) => {
  const idx = artifact.a.indexOf(artifact.g)
  if (idx === 0) {
    return toArtifactString({...artifact, a: `[...]${artifact.a.slice(artifact.g.length)}`})
  }
  return toArtifactString(artifact)
}

const Listbox = ({loading, ac}: ListboxProps) => {
  if (!ac.popupOpen || ac.inputValue === '') {
    return null
  }
  const noResultsFound = !loading && ac.groupedOptions.length === 0
  return (
    <div className='relative cursor-pointer'>
      <ul className='absolute flex flex-col w-full max-h-96 overflow-y-auto gap-0.5 border-2 border-emerald-700' {...ac.getListboxProps()}>
        {loading && <li>Loading...</li>}
        {noResultsFound && <li>No results found</li>}
        {/*{([{g: 'test', a: 'siema'}] as Artifact[]).map((option, index) => (*/}
         {(ac.groupedOptions as Artifact[]).map((option, index) => (
          <li className='truncate flex-shrink-0 p-2 bg-amber-100'
              {...ac.getOptionProps({option, index})}
              title={toArtifactString(option)}>
            {toShortArtifactString(option)}
          </li>
        ))}
      </ul>
    </div>
  )
}

export const Autocomplete = () => {
  const [inputValue, setInputValue] = useState('')
  const [debouncedInput] = useDebounce(inputValue, 500)
  const [options, setOptions] = useState<Artifact[]>([])
  const navigate = useNavigate()

  const onPackageSelect = (val: Artifact | null) => {
    if (val !== null) {
      navigate(`/packages/${toArtifactString(val)}:${val.latestVersion}`)
    }
  }
  const {
    data,
    loading,
    error,
    get
  } = useFetch<Artifact[]>('/api/v1/maven/search')

  useEffect(() => {
    if (debouncedInput !== '') {
      get('?query=' + inputValue)
    }
  }, [debouncedInput])

  useEffect(() => setOptions(data ?? []), [data])
  useEffect(() => {
    if (error || inputValue === '') {
      setOptions([])
    }
  }, [error, inputValue])

  const ac = useAutocomplete({
    id: 'packages-autocomplete',
    options: options,
    filterOptions: options => options,
    getOptionLabel: toArtifactString,
    isOptionEqualToValue: (a1, a2) => toArtifactString(a1) === toArtifactString(a2),
    onChange: (_event, newValue) => onPackageSelect(newValue),
    inputValue,
    onInputChange: (_event, newInputValue) => setInputValue(newInputValue),
    clearOnBlur: false,
    clearOnEscape: true,
    autoComplete: false
  })

  return (
    <div className='w-full flex flex-col font-mono' {...ac.getRootProps()}>
      {/*<label {...ac.getInputLabelProps()}>Hello</label>*/}
      <input className='h-10 p-1' {...ac.getInputProps()} value={inputValue}/>
      <Listbox loading={loading} ac={ac}/>
    </div>
  )
}