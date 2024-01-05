import {useAutocomplete} from '@mui/base'
import {useEffect, useState} from 'react'
import {CachePolicies, useFetch} from 'use-http'
import {useDebounce} from 'react-use'
import {UseAutocompleteReturnValue} from '@mui/base/useAutocomplete/useAutocomplete'
import {UseDebounceReturn} from 'react-use/lib/useDebounce'

interface Artifact {
  g: string
  a: string
}

interface ListboxProps {
  loading: boolean
  isDebounced: UseDebounceReturn[0]
  ac: UseAutocompleteReturnValue<Artifact>
}

const artifactString = (artifact: Artifact) => `${artifact.g}:${artifact.a}`

const Listbox = ({loading, isDebounced, ac}: ListboxProps) => {
  if (!ac.popupOpen) {
    return null
  }
  const noResultsFound = !loading && isDebounced() && ac.groupedOptions.length === 0
  return (
    <ul {...ac.getListboxProps()}>
      {loading && <div>Loading...</div>}
      {noResultsFound &&
        <div>No results found</div>
      }
      {(ac.groupedOptions as Artifact[]).map((option, index) => (
        <li {...ac.getOptionProps({option, index})}>
          {artifactString(option)}
        </li>
      ))}
    </ul>
  )
}

export const Autocomplete = () => {
  const [value, setValue] = useState<Artifact | null>(null)
  const [inputValue, setInputValue] = useState('')
  const [options, setOptions] = useState<Artifact[]>([])

  const {
    data,
    get,
    loading,
    error
  } = useFetch<Artifact[]>(`${import.meta.env.VITE_SERVER_URL}/api/v1/search`, {cachePolicy: CachePolicies.NO_CACHE})
  const [isDebounced] = useDebounce(() => {
    if (inputValue !== '') {
      get('?query=' + inputValue)
    }
  }, 500, [inputValue])
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
    getOptionLabel: artifactString,
    isOptionEqualToValue: (a1, a2) => artifactString(a1) === artifactString(a2),
    value,
    onChange: (_event, newValue) => setValue(newValue),
    inputValue,
    onInputChange: (_event, newInputValue) => setInputValue(newInputValue)
  })

  return (
    <div style={{marginBottom: 24}}>
      {value && <p>{artifactString(value)}</p>}
      <label {...ac.getInputLabelProps()}>Hello</label>
      <div {...ac.getRootProps()}>
        <input {...ac.getInputProps()} value={inputValue}/>
      </div>
      <Listbox loading={loading} isDebounced={isDebounced} ac={ac}/>
    </div>
  )
}