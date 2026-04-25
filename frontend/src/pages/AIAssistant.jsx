import React, { useState, useEffect } from 'react';
import './AIAssistant.css';

const AIAssistant = () => {
  const [isListening, setIsListening] = useState(true);
  const [pulse, setPulse] = useState(false);

  // Simulate pulse effect for AI speaking
  useEffect(() => {
    let interval;
    if (isListening) {
      interval = setInterval(() => {
        setPulse(p => !p);
      }, 1000);
    } else {
      setPulse(false);
    }
    return () => clearInterval(interval);
  }, [isListening]);

  return (
    <div className="focus-container ai-assistant-page">
      <div className="context-badge ai-badge">
        <span className={`badge-dot ${isListening ? 'active' : 'paused'}`}></span>
        AI Study Companion
      </div>

      <div className="ai-main-interaction">
        <div className={`ai-orb-container ${pulse ? 'pulse' : ''} ${!isListening ? 'paused' : ''}`}>
          <div className="ai-orb-ring ring-1"></div>
          <div className="ai-orb-ring ring-2"></div>
          <div className="ai-orb-ring ring-3"></div>
          <div className="ai-orb">
            <span className="material-symbols-outlined ai-orb-icon">
              {isListening ? 'graphic_eq' : 'mic_off'}
            </span>
          </div>
        </div>

        <div className="ai-transcript">
          <p className="ai-message h3">
            "I'm ready. Let's review Advanced Calculus."
          </p>
          <p className="user-message body-lg">
            Can you explain the chain rule again?
          </p>
        </div>
      </div>

      <div className="ai-controls">
        <button className="icon-btn-large secondary">
          <span className="material-symbols-outlined">settings</span>
        </button>
        <button 
          className={`btn-toggle-mic ${!isListening ? 'muted' : ''}`}
          onClick={() => setIsListening(!isListening)}
        >
          <span className="material-symbols-outlined filled">
            {isListening ? 'mic' : 'mic_off'}
          </span>
        </button>
        <button className="icon-btn-large error">
          <span className="material-symbols-outlined filled">call_end</span>
        </button>
      </div>
    </div>
  );
};

export default AIAssistant;
