import React, { useState, useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import Header from './Header'
import Sidebar from './Sidebar'

export const AdminLayout: React.FC = () => {
  const location = useLocation()
  const [isMobile, setIsMobile] = useState<boolean>(() => {
    return typeof window !== 'undefined' && window.innerWidth < 1024
  })

  const [isSidebarOpen, setIsSidebarOpen] = useState<boolean>(() => {
    if (typeof window !== 'undefined' && window.innerWidth < 1024) {
      return false
    }
    return localStorage.getItem('smartstay_sidebar_collapsed') !== 'true'
  })

  // Detect window resize to update mobile state
  useEffect(() => {
    const handleResize = () => {
      const mobile = window.innerWidth < 1024
      setIsMobile(mobile)
      if (mobile) {
        setIsSidebarOpen(false)
      }
    }
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  // Auto-close sidebar on mobile when navigating
  useEffect(() => {
    if (isMobile) {
      setIsSidebarOpen(false)
    }
  }, [location.pathname, isMobile])

  const toggleSidebar = () => {
    setIsSidebarOpen((prev) => {
      const next = !prev
      if (!isMobile) {
        try {
          localStorage.setItem('smartstay_sidebar_collapsed', String(!next))
        } catch {
          // ignore storage errors
        }
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
  }, [isMobile])

  return (
    <div className="h-screen bg-neutral-50 flex flex-col font-sans overflow-hidden">
      <Header isSidebarCollapsed={!isSidebarOpen} onToggleSidebar={toggleSidebar} />
      <div className="flex-1 flex overflow-hidden relative">
        {/* Mobile Backdrop */}
        {isMobile && isSidebarOpen && (
          <div
            className="fixed inset-0 z-40 bg-neutral-900/50 backdrop-blur-xs transition-opacity duration-300 lg:hidden"
            onClick={() => setIsSidebarOpen(false)}
            aria-hidden="true"
          />
        )}

        {/* Sidebar Container */}
        <div
          className={
            isMobile
              ? `fixed top-16 bottom-0 left-0 z-50 transition-transform duration-300 ease-in-out ${
                  isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
                }`
              : 'relative shrink-0'
          }
        >
          <Sidebar
            isCollapsed={!isSidebarOpen}
            onToggle={toggleSidebar}
            isMobile={isMobile}
            onCloseMobile={() => setIsSidebarOpen(false)}
          />
        </div>

        {/* Main Content Area */}
        <main className="flex-1 overflow-y-auto p-3 sm:p-4 md:p-6 lg:p-8 transition-all duration-300 w-full min-w-0">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default AdminLayout
