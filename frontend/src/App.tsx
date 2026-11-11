import { useEffect, useState } from "react";
import { ApiError, api } from "./api";
import "./App.css";
import Dashboard from "./DashboardChat";

function App() {
  const [isLogin, setIsLogin] = useState(true);

  const [isLoggedIn, setIsLoggedIn] = useState(
    localStorage.getItem("token") !== null
  );

  const [isCheckingAuth, setIsCheckingAuth] = useState(true);

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [message, setMessage] = useState("");


  // =========================
  // VERIFY EXISTING JWT
  // =========================

  useEffect(() => {
    const verifyToken = async () => {

      const token = localStorage.getItem("token");

      // No token means user is not logged in
      if (!token) {
        setIsLoggedIn(false);
        setIsCheckingAuth(false);
        return;
      }

      try {
        await api.get<{ email: string }>("/api/me");
        setIsLoggedIn(true);
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          setIsLoggedIn(false);
        }
        // If the backend cannot be reached, keep the existing login state.

      } finally {

        setIsCheckingAuth(false);

      }
    };

    verifyToken();

  }, []);


  // =========================
  // LOGIN
  // =========================

  const handleLogin = async () => {

    setMessage("");

    try {

      const data = await api.postJson<{
        token: string;
        email: string;
        fullName: string;
      }>("/api/auth/login", {
        email,
        password,
      }, true);

      localStorage.setItem("token", data.token);
      localStorage.setItem("fullName", data.fullName || "");
      localStorage.setItem("email", data.email || "");

      setIsLoggedIn(true);

    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setMessage("Invalid email or password.");
        return;
      }
      setMessage(error instanceof Error ? error.message : "Cannot connect to the server.");
    }
  };


  // =========================
  // REGISTER
  // =========================

  const handleRegister = async () => {

    setMessage("");

    try {

      const data = await api.postJson<{
        token: string;
        email: string;
        fullName: string;
      }>("/api/auth/register", {
        fullName,
        email,
        password,
      }, true);

      localStorage.setItem("token", data.token);
      localStorage.setItem("fullName", data.fullName || "");
      localStorage.setItem("email", data.email || "");

      setIsLoggedIn(true);

    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setMessage("This email is already registered.");
        return;
      }
      if (error instanceof ApiError && error.status === 400) {
        setMessage("Please check your name, email, and password (8+ characters).");
        return;
      }
      setMessage(error instanceof Error ? error.message : "Cannot connect to the server.");
    }
  };

  // =========================
  // CHECKING AUTHENTICATION
  // =========================

  if (isCheckingAuth) {

    return (
      <div
        style={{
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: "#080812",
          color: "#ffffff",
          fontSize: "16px",
        }}
      >
        Checking authentication...
      </div>
    );

  }


  // =========================
  // SHOW DASHBOARD
  // =========================

  if (isLoggedIn) {

    return <Dashboard />;

  }


  // =========================
  // LOGIN / REGISTER PAGE
  // =========================

  return (
    <div className="app">

      <div className="glow glow-one"></div>
      <div className="glow glow-two"></div>


      <div className="auth-container">


        {/* LEFT SIDE */}

        <div className="brand-section">

          <div className="logo">
            <span>âœ¦</span>
          </div>

          <h1>
            Doc<span>Mind</span>
          </h1>

          <p className="brand-description">
            Your intelligent document assistant.
            <br />
            Read, understand and interact with your documents.
          </p>


          <div className="feature-list">

            <div className="feature">

              <div className="feature-icon">
                ðŸ“„
              </div>

              <div>

                <strong>
                  Smart Documents
                </strong>

                <p>
                  Understand your documents faster.
                </p>

              </div>

            </div>


            <div className="feature">

              <div className="feature-icon">
                âœ¦
              </div>

              <div>

                <strong>
                  AI Powered
                </strong>

                <p>
                  Ask questions and get intelligent answers.
                </p>

              </div>

            </div>


            <div className="feature">

              <div className="feature-icon">
                âš¡
              </div>

              <div>

                <strong>
                  Fast & Simple
                </strong>

                <p>
                  Everything you need in one place.
                </p>

              </div>

            </div>

          </div>

        </div>


        {/* RIGHT SIDE */}

        <div className="auth-card">


          <div className="auth-header">

            <h2>

              {isLogin
                ? "Welcome back"
                : "Create your account"}

            </h2>

            <p>

              {isLogin
                ? "Sign in to continue to DocMind"
                : "Start your intelligent document journey"}

            </p>

          </div>


          {/* TABS */}

          <div className="auth-tabs">

            <button
              className={isLogin ? "active" : ""}
              onClick={() => {

                setIsLogin(true);
                setMessage("");

              }}
            >
              Login
            </button>


            <button
              className={!isLogin ? "active" : ""}
              onClick={() => {

                setIsLogin(false);
                setMessage("");

              }}
            >
              Register
            </button>

          </div>


          {/* FORM */}

          <div className="form">


            {/* FULL NAME */}

            {!isLogin && (

              <div className="input-group">

                <label>
                  Full Name
                </label>

                <input
                  type="text"
                  placeholder="Enter your full name"
                  value={fullName}
                  onChange={(e) =>
                    setFullName(e.target.value)
                  }
                />

              </div>

            )}


            {/* EMAIL */}

            <div className="input-group">

              <label>
                Email
              </label>

              <input
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) =>
                  setEmail(e.target.value)
                }
              />

            </div>


            {/* PASSWORD */}

            <div className="input-group">

              <label>
                Password
              </label>

              <input
                type="password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) =>
                  setPassword(e.target.value)
                }
              />

            </div>


            {/* FORGOT PASSWORD */}

            {isLogin && (

              <div className="forgot-password">
                Forgot password?
              </div>

            )}


            {/* SUBMIT BUTTON */}

            <button
              type="button"
              className="submit-button"
              onClick={() => {
                if (isLogin) {
                  handleLogin();
                } else {
                  handleRegister();
                }
              }}
            >

              {isLogin
                ? "Sign in"
                : "Create account"}

              <span>
                â†’
              </span>

            </button>

          </div>


          {/* MESSAGE */}

          {message && (

            <p
              style={{
                textAlign: "center",
                marginTop: "15px",
                color: "#8b83ff",
                fontSize: "13px",
              }}
            >
              {message}
            </p>

          )}


          {/* BOTTOM TEXT */}

          <p className="bottom-text">

            {isLogin
              ? "Don't have an account?"
              : "Already have an account?"}


            <button
              onClick={() => {

                setIsLogin(!isLogin);
                setMessage("");

              }}
            >

              {isLogin
                ? " Create one"
                : " Sign in"}

            </button>

          </p>


        </div>

      </div>


      {/* FOOTER */}

      <div className="footer">

        Â© 2026 DocMind Â· Intelligent Document Assistant

      </div>

    </div>
  );
}

export default App;
