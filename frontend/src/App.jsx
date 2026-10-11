import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import AdminLayout from './layouts/AdminLayout';
import PublicLayout from './layouts/PublicLayout';
import UserLayout from './layouts/UserLayout';
import AdminDashboard from './pages/AdminDashboard';
import Bookings from './pages/Bookings';
import Booking from './pages/Booking';
import Dashboard from './pages/Dashboard';
import Explore from './pages/Explore';
import Favorites from './pages/Favorites';
import Home from './pages/Home';
import LocationDetails from './pages/LocationDetails';
import Login from './pages/Login';
import ManageBookings from './pages/ManageBookings';
import ManageLocations from './pages/ManageLocations';
import ManageReviews from './pages/ManageReviews';
import ManageUsers from './pages/ManageUsers';
import NotFound from './pages/NotFound';
import Profile from './pages/Profile';
import Register from './pages/Register';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<PublicLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/explore" element={<Explore />} />
          <Route path="/locations/:id" element={<LocationDetails />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
        </Route>

        <Route element={<UserLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/bookings" element={<Bookings />} />
          <Route path="/locations/:id/book" element={<Booking />} />
          <Route path="/favorites" element={<Favorites />} />
          <Route path="/profile" element={<Profile />} />
        </Route>

        <Route element={<AdminLayout />}>
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/admin/locations" element={<ManageLocations />} />
          <Route path="/admin/bookings" element={<ManageBookings />} />
          <Route path="/admin/users" element={<ManageUsers />} />
          <Route path="/admin/reviews" element={<ManageReviews />} />
        </Route>

        <Route path="/404" element={<NotFound />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
