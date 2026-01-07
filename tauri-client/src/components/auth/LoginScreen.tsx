import { useState } from 'react';
import { authService } from '../../services/authService';
import './Auth.css';

interface LoginScreenProps {
  onNavigateToRegister: () => void;
}

const LoginScreen = ({ onNavigateToRegister }: LoginScreenProps) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      await authService.login(username, password);
    } catch (err: any) {
      console.error('Login error:', err);
      if (err.code === "ERR_NETWORK") {
        setError('Sunucuya erişilemiyor. (Backend kapalı olabilir)');
      } else if (err.response) {
        setError(`Giriş başarısız (${err.response.status}): ${err.response.data?.message || err.response.data?.error || 'Bilinmeyen hata'}`);
      } else {
        setError(`Hata: ${err.message}`);
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        {/* Logo */}
        <div className="auth-header">
          <img
            src="https://tailwindcss.com/plus-assets/img/logos/mark.svg?color=indigo&shade=600"
            alt="ChatTalkie"
            className="auth-logo"
          />
          <h2 className="auth-title">
            Hesabınıza giriş yapın
          </h2>
        </div>

        {/* Error Message */}
        {error && (
          <div className="auth-error">
            {error}
          </div>
        )}

        {/* Form */}
        <form className="auth-form" onSubmit={handleLogin}>
          <div className="form-group">
            <label htmlFor="username" className="form-label">
              Kullanıcı Adı
            </label>
            <input
              id="username"
              name="username"
              type="text"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="form-input"
              placeholder="Kullanıcı adınız"
            />
          </div>

          <div className="form-group">
            <label htmlFor="password" className="form-label">
              Şifre
            </label>
            <input
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="form-input"
              placeholder="Şifreniz"
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="auth-button"
          >
            {isLoading ? 'Giriş Yapılıyor...' : 'Giriş Yap'}
          </button>
        </form>

        {/* Register Link */}
        <p className="auth-footer">
          Henüz üye değil misiniz?
          <button
            onClick={onNavigateToRegister}
            className="auth-link"
          >
            Hemen kayıt olun
          </button>
        </p>
      </div>
    </div>
  );
};

export default LoginScreen;