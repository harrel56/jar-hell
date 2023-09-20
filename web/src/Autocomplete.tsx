import {useAutocomplete} from '@mui/base'
import {useState} from 'react'
import {CachePolicies, useFetch} from 'use-http'
import {useDebounce} from 'react-use'
import {UseAutocompleteReturnValue} from '@mui/base/useAutocomplete/useAutocomplete'

interface Artifact {
  g: string
  a: string
}

const artifactString = (artifact: Artifact) => `${artifact.g}:${artifact.a}`

const Listbox = ({ac}: {ac: UseAutocompleteReturnValue<Artifact>}) => {
  if (ac.groupedOptions.length > 0) {
    return (
      <ul {...ac.getListboxProps()}>
        {(ac.groupedOptions as Artifact[]).map((option, index) => (
          <li {...ac.getOptionProps({option, index})}>
            {artifactString(option)}
          </li>
        ))}
      </ul>
    )
  } else {
    return <div>popop</div>
  }
}

export const Autocomplete = () => {
  const [value, setValue] = useState<Artifact | null>()
  const [inputValue, setInputValue] = useState('')

  const {data, get, loading, error} = useFetch<Artifact[]>(`${import.meta.env.VITE_SERVER_URL}/api/v1/search`, {cachePolicy: CachePolicies.NO_CACHE})
  useDebounce(() => {
    if (inputValue !== '') {
      get('?query=' + inputValue)
    }
  }, 1000, [inputValue])
  console.log('e', error)
  console.log('d', data)
  const options = error ? [] : data ?? []

  const ac = useAutocomplete({
    id: 'use-autocomplete-demo',
    options: options,
    filterOptions: options => options,
    getOptionLabel: artifactString,
    value,
    onChange: (_event, newValue) => setValue(newValue),
    inputValue,
    onInputChange: (_event, newInputValue) => setInputValue(newInputValue),
  });

  return (
    <div style={{ marginBottom: 24 }}>
      <label {...ac.getInputLabelProps()}>Hello</label>
      <div {...ac.getRootProps()}>
        <input {...ac.getInputProps()} value={inputValue} />
      </div>
      <Listbox ac={ac} />
    </div>
  );
}