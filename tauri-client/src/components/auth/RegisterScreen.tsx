import { useState } from 'react';
import { authService } from '../../services/authService';
import './Auth.css';

interface RegisterScreenProps {
    onNavigateToLogin: () => void;
}

const RegisterScreen = ({ onNavigateToLogin }: RegisterScreenProps) => {
    const [name, setName] = useState('');
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const handleRegister = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);
        setError('');

        try {
            await authService.register(name, username, password);
            alert('Kayıt başarılı! Giriş yapabilirsiniz.');
            onNavigateToLogin();
        } catch (err: any) {
            if (err.code === "ERR_NETWORK") {
                setError('Sunucuya erişilemiyor.');
            } else if (err.response) {
                setError(err.response.data?.error || 'Kayıt başarısız.');
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
                        Yeni Hesap Oluştur
                    </h2>
                </div>

                {/* Error Message */}
                {error && (
                    <div className="auth-error">
                        {error}
                    </div>
                )}

                {/* Form */}
                <form className="auth-form" onSubmit={handleRegister}>
                    <div className="form-group">
                        <label htmlFor="name" className="form-label">
                            Ad Soyad
                        </label>
                        <input
                            id="name"
                            name="name"
                            type="text"
                            required
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            className="form-input"
                            placeholder="Adınız ve soyadınız"
                        />
                    </div>

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
                            required
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="form-input"
                            placeholder="Güçlü bir şifre seçin"
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={isLoading}
                        className="auth-button"
                    >
                        {isLoading ? 'Kayıt Yapılıyor...' : 'Kayıt Ol'}
                    </button>
                </form>

                {/* Login Link */}
                <p className="auth-footer">
                    Zaten üye misiniz?
                    <button
                        onClick={onNavigateToLogin}
                        className="auth-link"
                    >
                        Giriş yapın
                    </button>
                </p>
            </div>
        </div>
    );
};

export default RegisterScreen;