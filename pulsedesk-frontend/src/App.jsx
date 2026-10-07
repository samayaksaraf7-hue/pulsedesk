import { useState } from "react";
import Dashboard from "./Dashboard";
import "./App.css";

const API_URL = import.meta.env.VITE_API_URL;

function App() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [isLoggedIn, setIsLoggedIn] = useState(
      () =>
          !!localStorage.getItem("pulsedesk_token") ||
          !!sessionStorage.getItem("pulsedesk_token")
  );

  const handleLogin = async (event) => {
    event.preventDefault();

    setError("");

    if (!email.trim() || !password.trim()) {
      setError("Please enter your email and password.");
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(
          `${API_URL}/api/auth/login`,
          {
            method: "POST",

            headers: {
              "Content-Type": "application/json",
            },

            body: JSON.stringify({
              email: email.trim(),
              password: password,
            }),
          }
      );

      let data = {};

      try {
        data = await response.json();
      } catch {
        // Response did not contain JSON.
      }

      if (!response.ok) {
        throw new Error(
            data.message ||
            data.error ||
            "Invalid email or password."
        );
      }

      const token =
          data.token ||
          data.accessToken ||
          data.jwt;

      if (!token) {
        throw new Error(
            "Login succeeded but no JWT token was returned."
        );
      }

      if (rememberMe) {
        localStorage.setItem(
            "pulsedesk_token",
            token
        );

        sessionStorage.removeItem(
            "pulsedesk_token"
        );
      } else {
        sessionStorage.setItem(
            "pulsedesk_token",
            token
        );

        localStorage.removeItem(
            "pulsedesk_token"
        );
      }

      setIsLoggedIn(true);
    } catch (err) {
      console.error("Login error:", err);

      setError(
          err.message ||
          "Unable to connect to PulseDesk server."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem("pulsedesk_token");
    sessionStorage.removeItem("pulsedesk_token");

    setEmail("");
    setPassword("");
    setRememberMe(false);
    setError("");

    setIsLoggedIn(false);
  };

  /*
   * REAL DASHBOARD
   */
  if (isLoggedIn) {
    return (
        <Dashboard
            onLogout={handleLogout}
        />
    );
  }

  /*
   * LOGIN PAGE
   */
  return (
      <div className="login-page">
        <div className="glow glow-one"></div>
        <div className="glow glow-two"></div>

        <div className="login-container">

          {/* LEFT SIDE */}

          <section className="brand-section">

            <div className="logo">
              <span className="pulse-dot"></span>
              PulseDesk
            </div>

            <h1>
              Turn incidents into
              <span>
                {" "}intelligent action.
              </span>
            </h1>

            <p>
              Prioritize critical issues,
              balance team workload and
              resolve incidents faster from
              one intelligent workspace.
            </p>

            <div className="feature-row">

              <div className="feature-card">

                <div className="feature-icon">
                  ⚡
                </div>

                <strong>
                  Smart Priority
                </strong>

                <small>
                  Focus on what matters first
                </small>

              </div>

              <div className="feature-card">

                <div className="feature-icon">
                  ◎
                </div>

                <strong>
                  Auto Assignment
                </strong>

                <small>
                  Balance workload automatically
                </small>

              </div>

              <div className="feature-card">

                <div className="feature-icon">
                  ↗️
                </div>

                <strong>
                  Live Insights
                </strong>

                <small>
                  See team capacity instantly
                </small>

              </div>

            </div>

          </section>

          {/* LOGIN CARD */}

          <section className="login-card">

            <div className="status-badge">
              <span></span>
              SYSTEM ONLINE
            </div>

            <h2>
              Welcome back
            </h2>

            <p className="subtitle">
              Sign in to your PulseDesk workspace
            </p>

            <form onSubmit={handleLogin}>

              {/* EMAIL */}

              <label htmlFor="email">
                Email address
              </label>

              <div className="input-wrapper">

                <span>
                  ✉️
                </span>

                <input
                    id="email"
                    type="email"
                    placeholder="you@company.com"
                    value={email}
                    onChange={(event) =>
                        setEmail(event.target.value)
                    }
                    autoComplete="email"
                    disabled={loading}
                />

              </div>

              {/* PASSWORD */}

              <label htmlFor="password">
                Password
              </label>

              <div className="input-wrapper">

                <span>
                  ⌁
                </span>

                <input
                    id="password"
                    type="password"
                    placeholder="Enter your password"
                    value={password}
                    onChange={(event) =>
                        setPassword(event.target.value)
                    }
                    autoComplete="current-password"
                    disabled={loading}
                />

              </div>

              {/* ERROR */}

              {error && (
                  <div className="login-error">
                    ⚠️ {error}
                  </div>
              )}

              {/* OPTIONS */}

              <div className="form-options">

                <label className="remember">

                  <input
                      type="checkbox"
                      checked={rememberMe}
                      onChange={(event) =>
                          setRememberMe(
                              event.target.checked
                          )
                      }
                  />

                  Remember me

                </label>

                <button
                    type="button"
                    className="forgot"
                >
                  Forgot password?
                </button>

              </div>

              {/* LOGIN BUTTON */}

              <button
                  className="login-button"
                  type="submit"
                  disabled={loading}
              >

                {loading
                    ? "Signing in..."
                    : "Sign in to PulseDesk"
                }

                {!loading && (
                    <span>
                      →
                    </span>
                )}

              </button>

            </form>

            <div className="secure-message">

              <span>
                ◆
              </span>

              Secured with JWT authentication

            </div>

          </section>

        </div>

        <footer>
          <span className="footer-dot"></span>
          PulseDesk • Intelligent Incident Management
        </footer>

      </div>
  );
}

export default App;