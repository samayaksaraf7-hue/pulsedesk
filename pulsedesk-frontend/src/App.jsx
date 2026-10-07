import { useState } from "react";
import Dashboard from "./Dashboard";
import "./App.css";

const API_URL = import.meta.env.VITE_API_URL;

function App() {
  const [authMode, setAuthMode] = useState("login");

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] =
      useState("");

  const [rememberMe, setRememberMe] =
      useState(false);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [isLoggedIn, setIsLoggedIn] = useState(
      () =>
          !!localStorage.getItem("pulsedesk_token") ||
          !!sessionStorage.getItem("pulsedesk_token")
  );

  /*
   * STORE JWT TOKEN
   */
  const saveToken = (token) => {
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
  };

  /*
   * EXTRACT API ERROR MESSAGE
   */
  const getErrorMessage = (data, fallback) => {
    if (!data) {
      return fallback;
    }

    if (
        typeof data.message === "string" &&
        data.message.trim()
    ) {
      return data.message;
    }

    if (
        typeof data.error === "string" &&
        data.error.trim()
    ) {
      return data.error;
    }

    if (
        data.errors &&
        typeof data.errors === "object"
    ) {
      const messages = Object.values(
          data.errors
      );

      if (messages.length > 0) {
        return messages.join(" ");
      }
    }

    return fallback;
  };

  /*
   * LOGIN
   */
  const handleLogin = async (event) => {
    event.preventDefault();

    setError("");

    const cleanEmail =
        email.trim().toLowerCase();

    if (!cleanEmail || !password) {
      setError(
          "Please enter your email and password."
      );
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
              email: cleanEmail,
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
            getErrorMessage(
                data,
                "Invalid email or password."
            )
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

      saveToken(token);

      setIsLoggedIn(true);
    } catch (err) {
      console.error(
          "Login error:",
          err
      );

      setError(
          err.message ||
          "Unable to connect to PulseDesk server."
      );
    } finally {
      setLoading(false);
    }
  };

  /*
   * REGISTER
   */
  const handleRegister = async (event) => {
    event.preventDefault();

    setError("");

    const cleanName = name.trim();
    const cleanEmail =
        email.trim().toLowerCase();

    if (
        !cleanName ||
        !cleanEmail ||
        !password ||
        !confirmPassword
    ) {
      setError(
          "Please complete all registration fields."
      );
      return;
    }

    /*
     * NAME VALIDATION
     */
    if (
        cleanName.length < 2 ||
        cleanName.length > 50
    ) {
      setError(
          "Name must be between 2 and 50 characters."
      );
      return;
    }

    const namePattern =
        /^[\p{L}][\p{L} .'-]*$/u;

    if (!namePattern.test(cleanName)) {
      setError(
          "Name can contain only letters, spaces, apostrophes, hyphens and periods."
      );
      return;
    }

    /*
     * EMAIL VALIDATION
     */
    const emailPattern =
        /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

    if (!emailPattern.test(cleanEmail)) {
      setError(
          "Enter a valid email address."
      );
      return;
    }

    if (cleanEmail.length > 100) {
      setError(
          "Email must not exceed 100 characters."
      );
      return;
    }

    /*
     * PASSWORD VALIDATION
     */
    if (
        password.length < 8 ||
        password.length > 100
    ) {
      setError(
          "Password must be between 8 and 100 characters."
      );
      return;
    }

    if (password !== confirmPassword) {
      setError(
          "Passwords do not match."
      );
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(
          `${API_URL}/api/auth/register`,
          {
            method: "POST",

            headers: {
              "Content-Type": "application/json",
            },

            body: JSON.stringify({
              name: cleanName,
              email: cleanEmail,
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
            getErrorMessage(
                data,
                "Unable to create account."
            )
        );
      }

      const token =
          data.token ||
          data.accessToken ||
          data.jwt;

      if (!token) {
        throw new Error(
            "Account created, but no JWT token was returned."
        );
      }

      saveToken(token);

      setIsLoggedIn(true);
    } catch (err) {
      console.error(
          "Registration error:",
          err
      );

      setError(
          err.message ||
          "Unable to create your PulseDesk account."
      );
    } finally {
      setLoading(false);
    }
  };

  /*
   * CHANGE BETWEEN LOGIN / REGISTER
   */
  const switchMode = (mode) => {
    setAuthMode(mode);

    setError("");
    setPassword("");
    setConfirmPassword("");
  };

  /*
   * LOGOUT
   */
  const handleLogout = () => {
    localStorage.removeItem(
        "pulsedesk_token"
    );

    sessionStorage.removeItem(
        "pulsedesk_token"
    );

    setName("");
    setEmail("");
    setPassword("");
    setConfirmPassword("");
    setRememberMe(false);
    setError("");
    setAuthMode("login");

    setIsLoggedIn(false);
  };

  /*
   * DASHBOARD
   */
  if (isLoggedIn) {
    return (
        <Dashboard
            onLogout={handleLogout}
        />
    );
  }

  /*
   * AUTH PAGE
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

          {/* AUTH CARD */}

          <section className="login-card">

            <div className="status-badge">
              <span></span>
              SYSTEM ONLINE
            </div>

            <h2>
              {authMode === "login"
                  ? "Welcome back"
                  : "Create your account"}
            </h2>

            <p className="subtitle">
              {authMode === "login"
                  ? "Sign in to your PulseDesk workspace"
                  : "Join PulseDesk and start managing incidents"}
            </p>

            <div className="auth-switch">

              <button
                  type="button"
                  className={
                    authMode === "login"
                        ? "auth-switch-button active"
                        : "auth-switch-button"
                  }
                  onClick={() =>
                      switchMode("login")
                  }
                  disabled={loading}
              >
                Sign In
              </button>

              <button
                  type="button"
                  className={
                    authMode === "register"
                        ? "auth-switch-button active"
                        : "auth-switch-button"
                  }
                  onClick={() =>
                      switchMode("register")
                  }
                  disabled={loading}
              >
                Create Account
              </button>

            </div>

            <form
                onSubmit={
                  authMode === "login"
                      ? handleLogin
                      : handleRegister
                }
                noValidate
            >

              {authMode === "register" && (
                  <>
                    <label htmlFor="name">
                      Full name
                    </label>

                    <div className="input-wrapper">

                      <span>👤</span>

                      <input
                          id="name"
                          type="text"
                          placeholder="Your full name"
                          value={name}
                          onChange={(event) =>
                              setName(
                                  event.target.value
                              )
                          }
                          autoComplete="name"
                          maxLength={50}
                          disabled={loading}
                      />

                    </div>
                  </>
              )}

              <label htmlFor="email">
                Email address
              </label>

              <div className="input-wrapper">

                <span>✉️</span>

                <input
                    id="email"
                    type="email"
                    placeholder="you@company.com"
                    value={email}
                    onChange={(event) =>
                        setEmail(
                            event.target.value
                        )
                    }
                    autoComplete="email"
                    maxLength={100}
                    disabled={loading}
                />

              </div>

              <label htmlFor="password">
                Password
              </label>

              <div className="input-wrapper">

                <span>⌁</span>

                <input
                    id="password"
                    type="password"
                    placeholder={
                      authMode === "login"
                          ? "Enter your password"
                          : "Minimum 8 characters"
                    }
                    value={password}
                    onChange={(event) =>
                        setPassword(
                            event.target.value
                        )
                    }
                    autoComplete={
                      authMode === "login"
                          ? "current-password"
                          : "new-password"
                    }
                    disabled={loading}
                />

              </div>

              {authMode === "register" && (
                  <>
                    <label htmlFor="confirmPassword">
                      Confirm password
                    </label>

                    <div className="input-wrapper">

                      <span>✓</span>

                      <input
                          id="confirmPassword"
                          type="password"
                          placeholder="Enter password again"
                          value={confirmPassword}
                          onChange={(event) =>
                              setConfirmPassword(
                                  event.target.value
                              )
                          }
                          autoComplete="new-password"
                          disabled={loading}
                      />

                    </div>
                  </>
              )}

              {error && (
                  <div className="login-error">
                    ⚠️ {error}
                  </div>
              )}

              {authMode === "login" && (
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

                  </div>
              )}

              <button
                  className="login-button"
                  type="submit"
                  disabled={loading}
              >

                {loading
                    ? authMode === "login"
                        ? "Signing in..."
                        : "Creating account..."
                    : authMode === "login"
                        ? "Sign in to PulseDesk"
                        : "Create PulseDesk Account"
                }

                {!loading && (
                    <span>→</span>
                )}

              </button>

            </form>

            <div className="secure-message">
              <span>◆</span>
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