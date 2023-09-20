import {useAutocomplete} from '@mui/base'
import {useState} from 'react'
import {CachePolicies, useFetch} from 'use-http'
import {useDebounce} from 'react-use'

interface Artifact {
  g: string
  a: string
}

const artifactString = (artifact: Artifact) => `${artifact.g}:${artifact.a}`

export const Autocomplete = () => {
  const [value, setValue] = useState<Artifact | null>()
  const [inputValue, setInputValue] = useState('')
  const [options, setOptions] = useState<Artifact[]>([])

  const {get, loading, error} = useFetch<Artifact[]>(`${import.meta.env.VITE_SERVER_URL}/api/v1/search`, {cachePolicy: CachePolicies.NO_CACHE})
  useDebounce(() => {
    if (inputValue !== '') {
      get('?query=' + inputValue).then(artifacts => setOptions(artifacts))
    }
  }, 1000, [inputValue])
  console.log(options)

  const {
    getRootProps,
    getInputLabelProps,
    getInputProps,
    getListboxProps,
    getOptionProps,
    groupedOptions,
    focused,
  } = useAutocomplete({
    id: 'use-autocomplete-demo',
    options: options,
    getOptionLabel: artifactString,
    value,
    onChange: (_event, newValue) => setValue(newValue),
    inputValue,
    onInputChange: (_event, newInputValue) => setInputValue(newInputValue),
  });

  return (
    <div style={{ marginBottom: 24 }}>
      <label {...getInputLabelProps()}>Hello</label>
      <div
        {...getRootProps()}
        className={focused ? 'focused' : ''}
      >
        <input {...getInputProps()} value={inputValue} />
      </div>
      {groupedOptions.length > 0 && (
        <ul {...getListboxProps()}>
          {(groupedOptions as Artifact[]).map((option, index) => (
            <li {...getOptionProps({ option, index })}>
              {artifactString(option)}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}