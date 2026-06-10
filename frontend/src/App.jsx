import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom';
import TopAppBar from './components/TopAppBar';
import BottomNavBar from './components/BottomNavBar';
import Home from './pages/Home';
import Stats from './pages/Stats';
import Groups from './pages/Groups';
import GroupInfo from './pages/GroupInfo';
import Login from './pages/Login';
import Signup from './pages/Signup';
import AIAssistant from './pages/AIAssistant';
import { AuthProvider } from './context/AuthContext';
import { TimerProvider } from './context/TimerContext';
import './App.css';

const AppContent = () => {
  const location = useLocation();
  const isAuthPage = location.pathname === '/login' || location.pathname === '/signup';

  return (
    <>
      <TopAppBar />
      <main className="main-content container">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/assistant" element={<AIAssistant />} />
          <Route path="/stats" element={<Stats />} />
          <Route path="/groups" element={<Groups />} />
          <Route path="/groups/:id" element={<GroupInfo />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
        </Routes>
      </main>
      {!isAuthPage && <BottomNavBar />}
    </>
  );
};

function App() {
  return (
    <AuthProvider>
      <TimerProvider>
        <BrowserRouter>
          <AppContent />
        </BrowserRouter>
      </TimerProvider>
    </AuthProvider>

  );
}

export default App;
