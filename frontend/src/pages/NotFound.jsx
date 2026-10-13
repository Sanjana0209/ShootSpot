import { useNavigate } from 'react-router-dom';

function NotFound() {
  const navigate = useNavigate();

  return (
    <div className="not-found-page container">
      <div className="not-found-box">
        <p className="eyebrow">404</p>
        <h1>Page Not Found</h1>
        <button type="button" className="btn btn-primary" onClick={() => navigate('/')}>
          Back to Home
        </button>
      </div>
    </div>
  );
}

export default NotFound;
