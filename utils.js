export function entriesToObject(entries) {
  return Object.assign(...entries.map( ([k, v]) => ({[k]: v}) ))
}
