
import { useState } from "react";
import "./App.css";
import Dashboard from "./Dashboard";

function App() {
  const [isLogin, setIsLogin] = useState(true);

  const [isLoggedIn, setIsLoggedIn] = useState(
    localStorage.getItem("token") !== null
  );

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [message, setMessage] = useState("");

  // =========================
  // LOGIN
  // =========================

  const handleLogin = async () => {
    setMessage("");

    try {
      const response = await fetch(
        "http://localhost:8080/api/auth/login",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email: email,
            password: password,
          }),
        }
      );

      const data = await response.json();

      if (!response.ok) {
        setMessage("Invalid email or password.");
        return;
      }

      // Save JWT token
      localStorage.setItem("token", data.token);

      console.log("Logged in user:", data);

      setIsLoggedIn(true);

    } catch (error) {
      console.error(error);
      setMessage("Cannot connect to the server.");
    }
  };


  // =========================
  // REGISTER
  // =========================

  const handleRegister = async () => {
    setMessage("");

    try {
      const response = await fetch(
        "http://localhost:8080/api/auth/register",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            fullName: fullName,
            email: email,
            password: password,
          }),
        }
      );

      const data = await response.json();

      if (!response.ok) {

        if (response.status === 409) {
          setMessage("This email is already registered.");
        } else {
          setMessage("Registration failed.");
        }

        return;
      }

      // Registration successful
      localStorage.setItem("token", data.token);

      console.log("Registered user:", data);

      setIsLoggedIn(true);

    } catch (error) {
      console.error(error);
      setMessage("Cannot connect to the server.");
    }
  };


  // =========================
  // SHOW DASHBOARD
  // =========================

  if (isLoggedIn) {
    return <Dashboard />;
  }


  return (
    <div className="app">

      <div className="glow glow-one"></div>
      <div className="glow glow-two"></div>

      <div className="auth-container">

        {/* LEFT SIDE */}

        <div className="brand-section">

          <div className="logo">
            <span>✦</span>
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
              <div className="feature-icon">📄</div>

              <div>
                <strong>Smart Documents</strong>
                <p>Understand your documents faster.</p>
              </div>
            </div>

            <div className="feature">
              <div className="feature-icon">✦</div>

              <div>
                <strong>AI Powered</strong>
                <p>Ask questions and get intelligent answers.</p>
              </div>
            </div>

            <div className="feature">
              <div className="feature-icon">⚡</div>

              <div>
                <strong>Fast & Simple</strong>
                <p>Everything you need in one place.</p>
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

            {/* FULL NAME - REGISTER ONLY */}

            {!isLogin && (
              <div className="input-group">

                <label>Full Name</label>

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

              <label>Email</label>

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

              <label>Password</label>

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
                  console.log("LOGIN BUTTON CLICKED");
                  handleLogin();
                } else {
                  console.log("REGISTER BUTTON CLICKED");
                  handleRegister();
                }

              }}
            >

              {isLogin
                ? "Sign in"
                : "Create account"}

              <span>→</span>

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
        © 2026 DocMind · Intelligent Document Assistant
      </div>

    </div>
  );
}

export default App;
