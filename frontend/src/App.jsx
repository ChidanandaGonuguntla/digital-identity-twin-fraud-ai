import {useEffect, useState} from 'react';
import {BrowserRouter, Route, Routes} from 'react-router-dom';
import Sidebar from './components/layout/Sidebar';
import Topbar from './components/layout/Topbar';
import NotificationCenter from './components/notifications/NotificationCenter';
import ToastHost from './components/notifications/ToastHost';
import Operations from './pages/Operations';
import LiveFeed from './pages/LiveFeed';
import Assess from './pages/Assess';
import Twins from './pages/Twins';
import {useStore} from './store/useStore';

export default function App() {
    const start = useStore((s) => s.start);
    const markRead = useStore((s) => s.markRead);
    const [notifOpen, setNotifOpen] = useState(false);

    useEffect(() => {
        start();
    }, [start]);

    function openNotifications() {
        setNotifOpen(true);
        markRead();
    }

    return (
        <BrowserRouter future={{v7_relativeSplatPath: true, v7_startTransition: true}}>
            <div className="relative z-10 flex h-screen overflow-hidden">
                <Sidebar/>
                <div className="flex min-w-0 flex-1 flex-col">
                    <Topbar onBell={openNotifications}/>
                    <main className="flex-1 overflow-y-auto px-6 py-6">
                        <Routes>
                            <Route path="/" element={<Operations/>}/>
                            <Route path="/feed" element={<LiveFeed/>}/>
                            <Route path="/assess" element={<Assess/>}/>
                            <Route path="/twins" element={<Twins/>}/>
                        </Routes>
                    </main>
                </div>
            </div>

            <NotificationCenter open={notifOpen} onClose={() => setNotifOpen(false)}/>
            <ToastHost/>
        </BrowserRouter>
    );
}
