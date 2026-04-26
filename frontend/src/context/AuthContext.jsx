import { createContext, useContext, useEffect, useState } from 'react';
import axiosClient from "../lib/axios";

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  
  // fetch user 

  useEffect(() => {
    let mounted = true;
  
    const load = async () => {
      try {
        // try to refresh tokens first
        // await axiosClient.get('/auth/refresh-token', { withCredentials: true }).catch(() => {});
        // if error occurs, it means user is not authenticated, so we can just set user to null 
        const res = await axiosClient.get('/auth/me');
        if (mounted) setUser(res.data);
      } catch {
        if (mounted) setUser(null);
      } finally {
        if (mounted) setLoading(false);
      }
    };
  
    load();
  
    return () => { //clean up function 
      mounted = false;
    };
  }, []);
  

  const logout = async() => {
    await axiosClient.post('/auth/logout', {});
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, setUser, loading, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);


