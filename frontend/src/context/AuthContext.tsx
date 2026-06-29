import React, { createContext, useContext, useState, useEffect, useRef } from 'react'
import { api } from '../services/api.ts'
import axios from 'axios'

interface User {
  id: string
  username: string
  email: string
  eloRating: number
}

interface AuthContextType {
  user: User | null
  accessToken: string | null
  isAuthenticated: boolean
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  register: (username: string, email: string, password: string) => Promise<void>
  logout: () => void
  updateElo: (newElo: number) => void
}

const AuthContext = createContext<AuthContextType | null>(null)

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [user, setUser] = useState<User | null>(null)
  const [accessToken, setAccessToken] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const isRefreshingRef = useRef(false)
  const failedQueueRef = useRef<any[]>([])

  const processQueue = (error: any, token: string | null = null) => {
    failedQueueRef.current.forEach((prom) => {
      if (error) {
        prom.reject(error)
      } else {
        prom.resolve(token)
      }
    })
    failedQueueRef.current = []
  }

  // Silent login on start
  useEffect(() => {
    const initAuth = async () => {
      const storedRefreshToken = localStorage.getItem('refreshToken')
      if (storedRefreshToken) {
        try {
          const authBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
          const res = await axios.post(`${authBaseUrl}/auth/refresh`, {
            refreshToken: storedRefreshToken
          })
          const { accessToken: newAccessToken, refreshToken: newRefreshToken } = res.data
          setAccessToken(newAccessToken)
          localStorage.setItem('refreshToken', newRefreshToken)

          const profileRes = await api.get('/profile', {
            headers: { Authorization: `Bearer ${newAccessToken}` }
          })
          
          setUser({
            id: '',
            username: profileRes.data.username,
            email: '',
            eloRating: profileRes.data.eloRating
          })
        } catch (e) {
          console.error("Silent login failed", e)
          localStorage.removeItem('refreshToken')
        }
      }
      setLoading(false)
    }

    initAuth()
  }, [])

  // Request interceptor
  useEffect(() => {
    const requestInterceptor = api.interceptors.request.use(
      (config) => {
        if (accessToken) {
          config.headers.Authorization = `Bearer ${accessToken}`
        }
        return config
      },
      (error) => Promise.reject(error)
    )

    return () => {
      api.interceptors.request.eject(requestInterceptor)
    }
  }, [accessToken])

  // Response interceptor
  useEffect(() => {
    const responseInterceptor = api.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config
        
        if (error.response?.status === 401 && !originalRequest._retry) {
          if (isRefreshingRef.current) {
            return new Promise((resolve, reject) => {
              failedQueueRef.current.push({ resolve, reject })
            })
              .then((token) => {
                originalRequest.headers.Authorization = `Bearer ${token}`
                return api(originalRequest)
              })
              .catch((err) => Promise.reject(err))
          }

          originalRequest._retry = true
          isRefreshingRef.current = true

          const storedRefreshToken = localStorage.getItem('refreshToken')
          if (!storedRefreshToken) {
            isRefreshingRef.current = false
            logout()
            return Promise.reject(error)
          }

          try {
            const authBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
            const res = await axios.post(`${authBaseUrl}/auth/refresh`, {
              refreshToken: storedRefreshToken
            })
            const { accessToken: newAccessToken, refreshToken: newRefreshToken } = res.data
            setAccessToken(newAccessToken)
            localStorage.setItem('refreshToken', newRefreshToken)

            processQueue(null, newAccessToken)
            isRefreshingRef.current = false

            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
            return api(originalRequest)
          } catch (refreshError) {
            processQueue(refreshError, null)
            isRefreshingRef.current = false
            logout()
            return Promise.reject(refreshError)
          }
        }

        return Promise.reject(error)
      }
    )

    return () => {
      api.interceptors.response.eject(responseInterceptor)
    }
  }, [accessToken])

  const login = async (username: string, password: string) => {
    const res = await api.post('/auth/login', { username, password })
    const { accessToken: newAccessToken, refreshToken: newRefreshToken, eloRating, email } = res.data
    
    setAccessToken(newAccessToken)
    localStorage.setItem('refreshToken', newRefreshToken)
    setUser({
      id: res.data.id,
      username: res.data.username,
      email,
      eloRating
    })
  }

  const register = async (username: string, email: string, password: string) => {
    await api.post('/auth/register', { username, email, password })
  }

  const logout = () => {
    const storedRefreshToken = localStorage.getItem('refreshToken')
    if (storedRefreshToken) {
      api.post('/auth/logout', { refreshToken: storedRefreshToken }).catch(() => {})
    }
    setAccessToken(null)
    setUser(null)
    localStorage.removeItem('refreshToken')
  }

  const updateElo = (newElo: number) => {
    setUser((prev) => prev ? { ...prev, eloRating: newElo } : null)
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        accessToken,
        isAuthenticated: !!user,
        loading,
        login,
        register,
        logout,
        updateElo
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
