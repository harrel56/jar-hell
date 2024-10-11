import {useAutocomplete, UseAutocompleteReturnValue} from '@mui/base/useAutocomplete/useAutocomplete'
import React, {useLayoutEffect, useState} from 'react'
import {useDebounce} from 'use-debounce'
import {useFetch} from '../hooks/useFetch.ts'
import {Input} from '@/shadcn/components/ui/Input.tsx'
import {clsx} from 'clsx'
import {SearchIcon} from 'lucide-react'
import {LoadingSpinner} from '@/components/LoadingSpinner.tsx'
import {Link, useNavigate, useParams} from 'react-router-dom'
import {stringToGav} from '@/util.ts'

interface Artifact {
  g: string
  a: string
  latestVersion?: string
}

interface ListboxProps {
  ac: UseAutocompleteReturnValue<Artifact, false, false, true>
}

const toArtifactString = (artifact: Artifact | string) => {
  if (typeof artifact === 'string') {
    return artifact
  }
  return `${artifact.g}:${artifact.a}`
}
const toShortArtifactString = (artifact: Artifact) => {
  const idx = artifact.a.indexOf(artifact.g)
  if (idx === 0) {
    return toArtifactString({...artifact, a: `[...]${artifact.a.slice(artifact.g.length)}`})
  }
  return toArtifactString(artifact)
}

const isInputTheSameAsSelection = (input: string, selection: Artifact | null) =>
  input !== '' && input !== (selection && toArtifactString(selection))


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
    <div className='relative'>
      <ul
        className='absolute flex flex-col w-full max-h-[452px] z-10 overflow-y-auto mt-1.5 p-1 border rounded-md bg-background'
        {...ac.getListboxProps()}>
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

  useLayoutEffect(() => setOptions(data ?? []), [data])
  useLayoutEffect(() => {
    if (error || debouncedInput === '') {
      setOptions([])
    }
  }, [error, debouncedInput])

  useLayoutEffect(() => {
    if (gav) {
      const gavObject = stringToGav(gav)
      setInputValue(`${gavObject.groupId}:${gavObject.artifactId}`)
      const option = {g: gavObject.groupId, a: gavObject.artifactId}
      setSelectedValue(option)
      setOptions([])
    } else {
      setInputValue('')
      setSelectedValue(null)
    }
  }, [gav])

  useLayoutEffect(() => {
    if (isInputTheSameAsSelection(debouncedInput, selectedValue)) {
      get('?query=' + inputValue)
    }
  }, [debouncedInput])

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
      let artifact: Artifact | null
      if (typeof option === 'string') {
        const [group, artifactId] = option.split(':', 2)
        artifact = {g: group, a: artifactId ?? group}
      } else {
        artifact = option
      }

      if (artifact) {
        setOptions([])
        setSelectedValue(artifact)
        if (event.nativeEvent.type === 'keydown') {
          if (artifact.latestVersion) {
            navigate(`/packages/${toArtifactString(artifact)}:${artifact.latestVersion}`)
          } else {
            navigate(`/packages/${toArtifactString(artifact)}`)

          }
        }
      } else {
        setOptions([])
        setSelectedValue(null)
      }
    },
    clearOnBlur: false,
    clearOnEscape: true,
    autoComplete: false,
    freeSolo: true,
  })

  /* Well, hopefully this is right */
  const listboxVisible = ac.popupOpen && (ac.groupedOptions.length !== 0 ||
    (isInputTheSameAsSelection(debouncedInput, selectedValue) && inputValue === debouncedInput && !loading))

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