import React, { useState, useEffect } from 'react'
import { Outlet } from 'react-router-dom'
import Header from './Header'
import Sidebar from './Sidebar'

export const AdminLayout: React.FC = () => {
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState<boolean>(() => {
    return localStorage.getItem('smartstay_sidebar_collapsed') === 'true'
  })

  const toggleSidebar = () => {
    setIsSidebarCollapsed((prev) => {
      const next = !prev
      try {
        localStorage.setItem('smartstay_sidebar_collapsed', String(next))
      } catch {
        // ignore storage errors
      }
      return next
    })
  }

  // Keyboard shortcut Alt + S to toggle sidebar
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.altKey && (e.key === 's' || e.key === 'S')) {
        e.preventDefault()
        toggleSidebar()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  return (
    <div className="h-screen bg-neutral-50 flex flex-col font-sans overflow-hidden">
      <Header isSidebarCollapsed={isSidebarCollapsed} onToggleSidebar={toggleSidebar} />
      <div className="flex-1 flex overflow-hidden">
        <Sidebar isCollapsed={isSidebarCollapsed} onToggle={toggleSidebar} />
        <main className="flex-1 overflow-y-auto p-4 md:p-6 lg:p-8 transition-all duration-300">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AdminLayout
