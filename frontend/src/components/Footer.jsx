import { Link } from 'react-router-dom';

function Footer() {
  return (
    <footer className="site-footer">
      <div className="container footer-grid">
        <div>
          <h3>ShootSpot</h3>
          <p>
            Discover premium spaces for photography, reels, short films and storytelling.
          </p>
        </div>

        <div>
          <h4>Quick Links</h4>
          <ul>
            <li>
              <Link to="/">Home</Link>
            </li>
            <li>
              <Link to="/explore">Explore</Link>
            </li>
            <li>
              <Link to="/dashboard">Dashboard</Link>
            </li>
          </ul>
        </div>

        <div>
          <h4>Categories</h4>
          <ul>
            <li>Photoshoots</li>
            <li>Reels</li>
            <li>Short Films</li>
            <li>Advertisements</li>
          </ul>
        </div>

        <div>
          <h4>Contact</h4>
          <ul>
            <li>hello@shootspot.com</li>
            <li>+91 98765 43210</li>
            <li>Hyderabad, India</li>
          </ul>
        </div>
      </div>

      <div className="container footer-bottom">
        <p>© 2026 ShootSpot. All rights reserved.</p>
      </div>
    </footer>
  );
}

export default Footer;
