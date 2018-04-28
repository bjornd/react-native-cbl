export function entriesToObject(entries = []) {
  return entries.length ? Object.assign(...entries.map( ([k, v]) => ({[k]: v}) )) : {}
}
