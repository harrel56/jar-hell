import {useAutocomplete, UseAutocompleteReturnValue} from '@mui/base/useAutocomplete/useAutocomplete'
import React, {useEffect, useState} from 'react'
import {useDebounce} from 'use-debounce'
import {useNavigate} from 'react-router-dom'
import {useFetch} from './hooks/useFetch.ts'
import {Input} from '@/components/ui/Input.tsx'
import {clsx} from 'clsx'
import {SearchIcon} from 'lucide-react'
import {LoadingSpinner} from '@/LoadingSpinner.tsx'

interface Artifact {
  g: string
  a: string
  latestVersion: string
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
    <li className={clsx('truncate', 'flex-shrink-0', 'p-4', 'rounded-md', 'mui-focused:bg-input', selectable && ['hover:bg-input', 'cursor-pointer'])}
        {...props}>
      {children}
    </li>
  )
}

const Listbox = ({ac}: ListboxProps) => {
  if (!ac.popupOpen || ac.inputValue === '') {
    return null
  }
  // ac.groupedOptions = [{g: 'test', a: 'siema'}, {g: 'test2', a: 'siema2'}, {g: 'test3', a: 'siema3'}, {g: 'test', a: 'siema'}, {g: 'test2', a: 'siema2'}, {g: 'test3', a: 'siema3'}, {g: 'test', a: 'siema'}, {g: 'test2', a: 'siema2'}, {g: 'test3', a: 'siema3'}, {g: 'test', a: 'siema'}, {g: 'test2', a: 'siema2'}, {g: 'test3', a: 'siema3'}] as any
  return (
    <div className='relative mt-1.5'>
      <ul className='absolute flex flex-col w-full max-h-[452px] overflow-y-auto p-1 border rounded-md' {...ac.getListboxProps()}>
        {ac.groupedOptions.length === 0 && <ListboxOption selectable={false}>No results found</ListboxOption>}
         {(ac.groupedOptions as Artifact[]).map((option, index) => (
          <ListboxOption{...ac.getOptionProps({option, index})} title={toArtifactString(option)} key={toArtifactString(option)}>
            {toShortArtifactString(option)}
          </ListboxOption>
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
    <div className='lg:w-[1000px] md:w-full m-auto pt-8 w-full flex flex-col font-mono' {...ac.getRootProps()}>
      <Input className='h-16 pl-6 text-2xl'
             {...ac.getInputProps()}
             value={inputValue}
             placeholder='Search for a dependency...'
             autoFocus
             EndIcon={loading ? LoadingSpinner : SearchIcon}/>
      <Listbox ac={ac}/>
    </div>
  )
}