import { useState } from 'react'
import Map from './componenets/Map' 
import TowerMapPage from './componenets/Map'

function App() {
  const [count, setCount] = useState(0)

  return (
    <>
     <TowerMapPage/>
    </>
  )
}

export default App
