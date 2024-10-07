import {useAutocomplete, UseAutocompleteReturnValue} from '@mui/base/useAutocomplete/useAutocomplete'
import React, {useLayoutEffect, useState} from 'react'
import {useDebounce} from 'use-debounce'
import {useFetch} from './hooks/useFetch.ts'
import {Input} from '@/components/ui/Input.tsx'
import {clsx} from 'clsx'
import {SearchIcon} from 'lucide-react'
import {LoadingSpinner} from '@/LoadingSpinner.tsx'
import {Link, useNavigate, useParams} from 'react-router-dom'
import {stringToGav} from '@/util.ts'

interface Artifact {
  g: string
  a: string
  latestVersion?: string
}

interface ListboxProps {
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

const ListboxOption = ({children, selectable = true, ...props}: React.PropsWithChildren<any>) => {
  return (
    <li className={clsx('truncate', 'flex-shrink-0', 'p-4', 'rounded-md', 'mui-focused:bg-input', 'transition-colors',
      selectable && ['hover:bg-input', 'cursor-pointer'])} {...props}>
      {children}
    </li>
  )
}

const Listbox = ({ac}: ListboxProps) => {
  // ac.groupedOptions = [{g: 'test', a: 'siema'}, {g: 'test2', a: 'siema2'}, {g: 'test3', a: 'siema3'}, {g: 'test', a: 'siema'}, {g: 'test2', a: 'siema2'}, {g: 'test3', a: 'siema3'}, {g: 'test', a: 'siema'}, {g: 'test2', a: 'siema2'}, {g: 'test3', a: 'siema3'}, {g: 'test', a: 'siema'}, {g: 'test2', a: 'siema2'}, {g: 'test3', a: 'siema3'}] as any
  return (
    <div className='relative mt-1.5'>
      <ul
        className='absolute flex flex-col w-full max-h-[452px] overflow-y-auto p-1 border rounded-md' {...ac.getListboxProps()}>
        {ac.groupedOptions.length === 0 && <ListboxOption selectable={false}>No results found</ListboxOption>}
        {(ac.groupedOptions as Artifact[]).map((option, index) => {
          const artifactString = toArtifactString(option)
          const {key, ...optionProps} = ac.getOptionProps({option, index})
          return (
            <Link key={artifactString} to={`/packages/${artifactString}:${option.latestVersion}`} title={artifactString}>
              <ListboxOption {...optionProps}>{toShortArtifactString(option)}</ListboxOption>
            </Link>
          )
        })}
      </ul>
    </div>
  )
}

export const Autocomplete = () => {
  const [inputValue, setInputValue] = useState('')
  const [selectedValue, setSelectedValue] = useState<Artifact | null>(null)
  const [debouncedInput] = useDebounce(inputValue, 500)
  const [options, setOptions] = useState<Artifact[]>([])
  const { gav } = useParams()
  const navigate = useNavigate()

  const {
    data,
    loading,
    error,
    get
  } = useFetch<Artifact[]>('/api/v1/maven/search')

  useLayoutEffect(() => {
    const gavObject = gav && stringToGav(gav)
    if (gavObject) {
      setInputValue(`${gavObject.groupId}:${gavObject.artifactId}`)
      const option = {g: gavObject.groupId, a: gavObject.artifactId}
      setSelectedValue(option)
      setOptions([option])
    } else {
      setInputValue('')
      setSelectedValue(null)
    }
  }, [gav])

  useLayoutEffect(() => {
    /* Don't make requests when input is the same as selection */
    if (debouncedInput !== (selectedValue && toArtifactString(selectedValue)) && debouncedInput !== '') {
      get('?query=' + inputValue)
    }
  }, [debouncedInput])

  useLayoutEffect(() => setOptions(data ?? []), [data])
  useLayoutEffect(() => {
    if (error || debouncedInput === '') {
      setOptions([])
    }
  }, [error, debouncedInput])

  const ac = useAutocomplete({
    id: 'packages-autocomplete',
    options: options,
    filterOptions: options => options,
    getOptionLabel: toArtifactString,
    getOptionKey: toArtifactString,
    isOptionEqualToValue: (a1, a2) => toArtifactString(a1) === toArtifactString(a2),
    inputValue,
    onInputChange: (_event, newInputValue) => setInputValue(newInputValue),
    value: selectedValue,
    onChange: (event, option) => {
      if (option) {
        setOptions([option])
        setSelectedValue(option)
        if (event.nativeEvent.type === 'keydown') {
          navigate(`/packages/${toArtifactString(option)}:${option.latestVersion}`)
        }
      } else {
        setSelectedValue(null)
      }
    },
    clearOnBlur: false,
    clearOnEscape: true,
    autoComplete: false
  })

  /* Well, hopefully this is right */
  const listboxVisible = ac.popupOpen &&
    (ac.groupedOptions.length !== 0 || (inputValue === debouncedInput && debouncedInput !== '' && !loading))

  return (
    <div className='lg:w-[1000px] md:w-full m-auto pt-8 w-full flex flex-col font-mono' {...ac.getRootProps()}>
      <Input className='h-16 pl-6 text-2xl'
             {...ac.getInputProps()}
             value={inputValue}
             placeholder='Search for a dependency...'
             autoFocus
             EndIcon={loading ? LoadingSpinner : SearchIcon}/>
      {listboxVisible && <Listbox ac={ac}/>}
    </div>
  )
}