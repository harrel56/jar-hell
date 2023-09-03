import {useAutocomplete} from '@mui/base'
import {useState} from 'react'
import {CachePolicies, useFetch} from 'use-http'

export const Autocomplete = () => {
  const [value, setValue] = useState<string | null>('')
  const [options, setOptions] = useState<string[]>([])

  const {get, loading, error} = useFetch<any>('/api/v1/search', {cachePolicy: CachePolicies.NO_CACHE})


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
    onInputChange: (_e, v) => {
      // if (v && !options.includes(v)) {
      //   setOptions([...options, v])
      // }
      get('?query=' + v).then(val => console.log(val))
    },
    // getOptionLabel: (option) => option.label,
    value,
    onChange: (_event, newValue) => setValue(newValue),
  });

  return (
    <div style={{ marginBottom: 24 }}>
      <label {...getInputLabelProps()}>Hello</label>
      <div
        {...getRootProps()}
        className={focused ? 'focused' : ''}
      >
        <input {...getInputProps()} />
      </div>
      {groupedOptions.length > 0 && (
        <ul {...getListboxProps()}>
          {(groupedOptions as string[]).map((option, index) => (
            <li {...getOptionProps({ option, index })}>
              {option}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}