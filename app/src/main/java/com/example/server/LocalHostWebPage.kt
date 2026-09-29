package com.example.server

object LocalHostWebPage {

    fun getHtml(deviceIp: String, port: Int, activeModel: String, isPasscodeProtected: Boolean = false): String {
        return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>OpenLLM Local Gateway</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
<style>
  :root {
    --bg-main: #0b0d13;
    --bg-surface: #121620;
    --bg-card: #181d2a;
    --bg-card-hover: #1f2536;
    --border: #232a3d;
    --border-subtle: #1c2231;
    --text-main: #f3f4f6;
    --text-muted: #94a3b8;
    --text-faint: #64748b;
    --accent: #f97316;
    --accent-gradient: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
    --accent-soft: rgba(249, 115, 22, 0.12);
    --accent-glow: rgba(249, 115, 22, 0.25);
    --user-bubble: #ea580c;
    --user-bubble-gradient: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
    --ai-bubble: #151923;
    --code-bg: #0d1017;
    --font: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    --font-mono: 'JetBrains Mono', Consolas, Monaco, monospace;
    --sidebar-width: 290px;
    --header-height: 60px;
    --radius-sm: 8px;
    --radius-md: 12px;
    --radius-lg: 16px;
    --radius-xl: 22px;
    --shadow-sm: 0 1px 2px rgba(0,0,0,0.2);
    --shadow-md: 0 4px 14px rgba(0,0,0,0.25);
    --shadow-lg: 0 12px 36px rgba(0,0,0,0.4);
  }
  [data-theme="light"] {
    --bg-main: #f8fafc;
    --bg-surface: #ffffff;
    --bg-card: #f1f5f9;
    --bg-card-hover: #e2e8f0;
    --border: #e2e8f0;
    --border-subtle: #edf2f7;
    --text-main: #0f172a;
    --text-muted: #64748b;
    --text-faint: #94a3b8;
    --accent: #f97316;
    --accent-gradient: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
    --accent-soft: rgba(249, 115, 22, 0.1);
    --accent-glow: rgba(249, 115, 22, 0.2);
    --user-bubble: #f97316;
    --user-bubble-gradient: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
    --ai-bubble: #ffffff;
    --code-bg: #f6f8fa;
    --shadow-sm: 0 1px 2px rgba(0,0,0,0.05);
    --shadow-md: 0 4px 12px rgba(0,0,0,0.06);
    --shadow-lg: 0 10px 30px rgba(0,0,0,0.08);
  }

  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: var(--font);
    background: var(--bg-main);
    color: var(--text-main);
    display: flex;
    height: 100vh;
    width: 100vw;
    overflow: hidden;
    -webkit-font-smoothing: antialiased;
  }

  /* Custom Scrollbars */
  ::-webkit-scrollbar { width: 6px; height: 6px; }
  ::-webkit-scrollbar-track { background: transparent; }
  ::-webkit-scrollbar-thumb { background: rgba(148, 163, 184, 0.2); border-radius: 4px; }
  ::-webkit-scrollbar-thumb:hover { background: rgba(148, 163, 184, 0.35); }

  /* Sidebar */
  #sidebar {
    width: var(--sidebar-width);
    background: var(--bg-surface);
    border-right: 1px solid var(--border);
    display: flex;
    flex-direction: column;
    transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), width 0.2s ease, margin-left 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    z-index: 50;
    flex-shrink: 0;
  }
  #sidebar.collapsed {
    margin-left: calc(-1 * var(--sidebar-width));
  }
  .sidebar-header {
    height: var(--header-height);
    padding: 0 16px;
    border-bottom: 1px solid var(--border);
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .brand {
    display: flex;
    align-items: center;
    gap: 10px;
    font-weight: 700;
    font-size: 15px;
    letter-spacing: -0.2px;
    color: var(--text-main);
  }
  .brand-logo-icon {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    background: var(--accent-gradient);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 15px;
    box-shadow: 0 2px 8px var(--accent-glow);
  }
  .sidebar-toggle-btn {
    background: none;
    border: none;
    color: var(--text-muted);
    cursor: pointer;
    padding: 6px;
    border-radius: var(--radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.15s;
  }
  .sidebar-toggle-btn:hover {
    background: var(--bg-card);
    color: var(--text-main);
  }

  .sidebar-action-wrap {
    padding: 12px 14px 8px 14px;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .new-chat-btn {
    width: 100%;
    padding: 10px 14px;
    background: var(--accent-gradient);
    color: #ffffff;
    border: none;
    border-radius: var(--radius-md);
    font-weight: 600;
    font-size: 13.5px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    box-shadow: 0 2px 10px var(--accent-glow);
    transition: all 0.18s;
  }
  .new-chat-btn:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 14px var(--accent-glow);
    filter: brightness(1.05);
  }
  .new-chat-btn:active { transform: translateY(0); }

  .search-box {
    position: relative;
    width: 100%;
  }
  .search-box input {
    width: 100%;
    padding: 7px 10px 7px 30px;
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    color: var(--text-main);
    font-size: 12.5px;
    outline: none;
    transition: border-color 0.15s;
  }
  .search-box input:focus { border-color: var(--accent); }
  .search-box svg {
    position: absolute;
    left: 9px;
    top: 50%;
    transform: translateY(-50%);
    width: 14px;
    height: 14px;
    color: var(--text-faint);
    pointer-events: none;
  }

  .sessions-header {
    padding: 8px 16px 4px 16px;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.6px;
    color: var(--text-faint);
    display: flex;
    justify-content: space-between;
  }

  .sessions-list {
    flex: 1;
    overflow-y: auto;
    padding: 4px 10px 12px 10px;
  }
  .session-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 9px 12px;
    margin-bottom: 3px;
    border-radius: var(--radius-md);
    cursor: pointer;
    color: var(--text-muted);
    font-size: 13px;
    font-weight: 500;
    transition: all 0.15s;
    gap: 8px;
    border: 1px solid transparent;
  }
  .session-item:hover {
    background: var(--bg-card);
    color: var(--text-main);
  }
  .session-item.active {
    background: var(--accent-soft);
    color: var(--accent);
    font-weight: 600;
    border-color: rgba(249, 115, 22, 0.25);
  }
  .session-icon {
    font-size: 14px;
    opacity: 0.7;
    flex-shrink: 0;
  }
  .session-title {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .session-delete {
    opacity: 0;
    background: none;
    border: none;
    color: var(--text-faint);
    cursor: pointer;
    padding: 3px 5px;
    font-size: 12px;
    border-radius: 4px;
    transition: all 0.15s;
  }
  .session-item:hover .session-delete { opacity: 1; }
  .session-delete:hover {
    color: #ef4444;
    background: rgba(239, 68, 68, 0.12);
  }

  .sidebar-footer {
    padding: 12px 14px;
    border-top: 1px solid var(--border);
    font-size: 11.5px;
    color: var(--text-muted);
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: var(--bg-surface);
  }
  .host-status {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 500;
  }
  .pulse-dot {
    width: 8px;
    height: 8px;
    background: #10b981;
    border-radius: 50%;
    box-shadow: 0 0 8px #10b981;
    animation: pulse 2s infinite;
  }
  @keyframes pulse {
    0%, 100% { transform: scale(1); opacity: 1; }
    50% { transform: scale(1.2); opacity: 0.7; }
  }

  /* Main Workspace */
  #main {
    flex: 1;
    display: flex;
    flex-direction: column;
    height: 100vh;
    min-width: 0;
    position: relative;
    background: var(--bg-main);
  }

  /* Chat Header */
  .chat-header {
    height: var(--header-height);
    border-bottom: 1px solid var(--border);
    background: var(--bg-surface);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    flex-shrink: 0;
    gap: 12px;
  }
  .header-left, .header-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .nav-btn {
    background: none;
    border: 1px solid transparent;
    color: var(--text-muted);
    cursor: pointer;
    padding: 7px;
    border-radius: var(--radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.15s;
  }
  .nav-btn:hover {
    background: var(--bg-card);
    color: var(--text-main);
    border-color: var(--border);
  }

  /* Model Switcher Pill */
  .model-pill-container {
    display: flex;
    align-items: center;
    gap: 8px;
    background: var(--bg-card);
    border: 1px solid var(--border);
    padding: 5px 12px;
    border-radius: var(--radius-xl);
    transition: all 0.15s;
  }
  .model-pill-container:hover {
    border-color: var(--accent);
    background: var(--bg-card-hover);
  }
  .model-chip-tag {
    font-size: 10.5px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.6px;
    background: var(--accent-soft);
    color: var(--accent);
    padding: 2px 7px;
    border-radius: 6px;
  }
  select.model-select {
    background: transparent;
    border: none;
    color: var(--text-main);
    font-size: 13px;
    font-weight: 600;
    outline: none;
    cursor: pointer;
    max-width: 240px;
  }
  select.model-select option {
    background: var(--bg-surface);
    color: var(--text-main);
  }

  /* Header action buttons */
  .header-action-btn {
    background: var(--bg-card);
    border: 1px solid var(--border);
    color: var(--text-main);
    padding: 6px 12px;
    border-radius: var(--radius-md);
    font-size: 12.5px;
    font-weight: 500;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 6px;
    transition: all 0.15s;
  }
  .header-action-btn:hover {
    background: var(--accent-soft);
    border-color: var(--accent);
    color: var(--accent);
  }
  .header-action-btn.icon-only {
    padding: 7px;
  }

  /* Chat Messages Container */
  .chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 28px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: 0;
  }
  .messages-flow {
    width: 100%;
    max-width: 860px;
    display: flex;
    flex-direction: column;
    gap: 22px;
  }

  /* Welcome Banner */
  .welcome-banner {
    margin: auto;
    max-width: 680px;
    text-align: center;
    padding: 48px 20px 20px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  .welcome-badge {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: var(--accent-soft);
    color: var(--accent);
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 16px;
    border: 1px solid rgba(249, 115, 22, 0.2);
  }
  .welcome-banner h2 {
    color: var(--text-main);
    font-size: 28px;
    font-weight: 800;
    letter-spacing: -0.5px;
    margin-bottom: 10px;
  }
  .welcome-banner p {
    font-size: 14.5px;
    line-height: 1.6;
    color: var(--text-muted);
    max-width: 540px;
    margin-bottom: 28px;
  }
  .starter-cards {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    width: 100%;
    margin-top: 8px;
  }
  .starter-card {
    background: var(--bg-surface);
    border: 1px solid var(--border);
    padding: 14px 16px;
    border-radius: var(--radius-lg);
    text-align: left;
    cursor: pointer;
    transition: all 0.18s;
  }
  .starter-card:hover {
    border-color: var(--accent);
    background: var(--bg-card);
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }
  .starter-card .icon { font-size: 20px; margin-bottom: 6px; display: block; }
  .starter-card .title { font-weight: 600; font-size: 13.5px; color: var(--text-main); margin-bottom: 2px; }
  .starter-card .desc { font-size: 11.5px; color: var(--text-muted); line-height: 1.4; }

  /* Message Rows */
  .message-row {
    display: flex;
    gap: 14px;
    width: 100%;
    align-items: flex-start;
  }
  .message-row.user {
    justify-content: flex-end;
  }
  .message-row.assistant {
    justify-content: flex-start;
  }

  .avatar {
    width: 34px;
    height: 34px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    font-weight: 700;
    flex-shrink: 0;
    user-select: none;
  }
  .user-avatar {
    background: var(--user-bubble-gradient);
    color: #ffffff;
    box-shadow: 0 2px 8px var(--accent-glow);
  }
  .ai-avatar {
    background: var(--bg-card);
    border: 1px solid var(--border);
    color: #10b981;
  }

  .message-body-wrap {
    display: flex;
    flex-direction: column;
    max-width: 82%;
  }
  .message-row.user .message-body-wrap {
    align-items: flex-end;
  }
  .message-row.assistant .message-body-wrap {
    align-items: flex-start;
    width: 100%;
    max-width: calc(100% - 48px);
  }

  .bubble {
    padding: 14px 18px;
    border-radius: var(--radius-lg);
    font-size: 14.5px;
    line-height: 1.65;
    word-break: break-word;
    box-shadow: var(--shadow-sm);
  }
  .message-row.user .bubble {
    background: var(--user-bubble-gradient);
    color: #ffffff;
    border-bottom-right-radius: 4px;
    font-weight: 450;
  }
  .message-row.assistant .bubble {
    background: var(--ai-bubble);
    color: var(--text-main);
    border: 1px solid var(--border);
    border-bottom-left-radius: 4px;
    width: 100%;
  }

  /* Rich Markdown Styling Inside AI Bubble */
  .bubble h1, .bubble h2, .bubble h3, .bubble h4 {
    color: var(--text-main);
    font-weight: 700;
    margin: 14px 0 8px 0;
    line-height: 1.3;
  }
  .bubble h1:first-child, .bubble h2:first-child, .bubble h3:first-child { margin-top: 0; }
  .bubble h1 { font-size: 20px; }
  .bubble h2 { font-size: 17px; }
  .bubble h3 { font-size: 15px; }

  .bubble p {
    margin-bottom: 10px;
  }
  .bubble p:last-child {
    margin-bottom: 0;
  }

  .bubble ul, .bubble ol {
    margin: 8px 0 12px 22px;
  }
  .bubble li {
    margin-bottom: 6px;
    line-height: 1.55;
  }

  .bubble strong {
    font-weight: 650;
    color: var(--text-main);
  }

  .bubble hr {
    border: none;
    border-top: 1px solid var(--border);
    margin: 14px 0;
  }

  .bubble blockquote {
    border-left: 3px solid var(--accent);
    padding: 6px 14px;
    margin: 10px 0;
    background: var(--accent-soft);
    border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
    color: var(--text-muted);
    font-style: italic;
  }

  /* Table Rendering */
  .bubble-table-wrapper {
    overflow-x: auto;
    margin: 12px 0;
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
  }
  .bubble table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;
    text-align: left;
  }
  .bubble th, .bubble td {
    padding: 8px 12px;
    border-bottom: 1px solid var(--border);
  }
  .bubble th {
    background: var(--bg-card);
    font-weight: 650;
    color: var(--text-main);
  }
  .bubble tr:last-child td { border-bottom: none; }
  .bubble tr:nth-child(even) td { background: rgba(148, 163, 184, 0.04); }

  /* Code Block Styling */
  .code-block-container {
    background: var(--code-bg);
    border: 1px solid var(--border);
    border-radius: var(--radius-md);
    margin: 12px 0;
    overflow: hidden;
  }
  .code-block-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 6px 12px;
    background: rgba(0,0,0,0.15);
    border-bottom: 1px solid var(--border);
    font-size: 11.5px;
    color: var(--text-muted);
    font-family: var(--font-mono);
  }
  .code-block-header span {
    text-transform: lowercase;
    font-weight: 500;
  }
  .copy-code-btn {
    background: transparent;
    border: 1px solid transparent;
    color: var(--text-muted);
    cursor: pointer;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 11.5px;
    display: flex;
    align-items: center;
    gap: 4px;
    transition: all 0.15s;
  }
  .copy-code-btn:hover {
    background: rgba(255,255,255,0.1);
    color: #fff;
  }
  .bubble pre {
    background: transparent;
    padding: 12px 14px;
    overflow-x: auto;
    font-family: var(--font-mono);
    font-size: 13px;
    line-height: 1.5;
    color: var(--text-main);
  }
  .bubble code {
    font-family: var(--font-mono);
    background: var(--code-bg);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 12.5px;
    border: 1px solid var(--border-subtle);
  }
  .bubble pre code {
    background: transparent;
    padding: 0;
    border: none;
    font-size: 13px;
  }

  /* Bubble Actions Bar (TTS, Copy, Retry) */
  .message-actions {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-top: 6px;
    opacity: 0.8;
    transition: opacity 0.15s;
  }
  .msg-act-btn {
    background: none;
    border: 1px solid transparent;
    color: var(--text-faint);
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 6px;
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 4px;
    transition: all 0.15s;
  }
  .msg-act-btn:hover {
    background: var(--bg-card);
    color: var(--text-main);
    border-color: var(--border);
  }
  .msg-act-btn.active {
    color: var(--accent);
  }

  .bubble-attachment {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: rgba(0,0,0,0.25);
    border: 1px solid rgba(255,255,255,0.25);
    padding: 4px 10px;
    border-radius: var(--radius-sm);
    font-size: 12px;
    margin-bottom: 8px;
  }

  /* Chat Input Section */
  .chat-input-container {
    padding: 12px 20px 22px 20px;
    background: var(--bg-surface);
    border-top: 1px solid var(--border);
    flex-shrink: 0;
    display: flex;
    justify-content: center;
  }
  .input-wrapper {
    width: 100%;
    max-width: 860px;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .input-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    color: var(--text-muted);
  }
  .web-search-chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 4px 10px;
    border-radius: 16px;
    background: var(--bg-card);
    border: 1px solid var(--border);
    cursor: pointer;
    user-select: none;
    font-weight: 500;
    font-size: 12px;
    transition: all 0.15s;
  }
  .web-search-chip:hover {
    border-color: var(--accent);
  }
  .web-search-chip.active {
    background: var(--accent-soft);
    color: var(--accent);
    border-color: rgba(249, 115, 22, 0.35);
  }
  .web-search-chip input { display: none; }

  .attachment-preview-bar {
    display: none;
    align-items: center;
    justify-content: space-between;
    background: var(--bg-card);
    border: 1px solid var(--border);
    padding: 8px 14px;
    border-radius: var(--radius-md);
    font-size: 12.5px;
    animation: fadeIn 0.15s ease;
  }
  @keyframes fadeIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
  .attachment-preview-bar.active { display: flex; }
  .attachment-info {
    display: flex;
    align-items: center;
    gap: 8px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .attachment-remove-btn {
    background: none;
    border: none;
    color: #ef4444;
    font-size: 14px;
    cursor: pointer;
    padding: 2px 6px;
    border-radius: 4px;
  }
  .attachment-remove-btn:hover { background: rgba(239, 68, 68, 0.1); }

  .input-box {
    display: flex;
    align-items: flex-end;
    background: var(--bg-card);
    border: 1.5px solid var(--border);
    border-radius: var(--radius-lg);
    padding: 8px 12px;
    gap: 8px;
    transition: all 0.18s;
    box-shadow: var(--shadow-sm);
  }
  .input-box:focus-within {
    border-color: var(--accent);
    box-shadow: 0 0 0 3px var(--accent-soft);
  }
  .attach-btn {
    background: none;
    border: none;
    color: var(--text-muted);
    font-size: 18px;
    cursor: pointer;
    padding: 6px;
    border-radius: var(--radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.15s;
  }
  .attach-btn:hover {
    color: var(--accent);
    background: var(--bg-card-hover);
  }
  textarea#userInput {
    flex: 1;
    background: transparent;
    border: none;
    color: var(--text-main);
    font-family: inherit;
    font-size: 14.5px;
    resize: none;
    outline: none;
    max-height: 180px;
    line-height: 1.55;
    padding: 6px 2px;
  }
  textarea#userInput::placeholder {
    color: var(--text-faint);
  }
  button#sendBtn {
    background: var(--accent-gradient);
    color: #ffffff;
    border: none;
    width: 38px;
    height: 38px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.18s;
    flex-shrink: 0;
    box-shadow: 0 2px 8px var(--accent-glow);
  }
  button#sendBtn:hover:not(:disabled) {
    transform: scale(1.05);
    filter: brightness(1.08);
  }
  button#sendBtn:active:not(:disabled) { transform: scale(0.95); }
  button#sendBtn:disabled { opacity: 0.45; cursor: not-allowed; box-shadow: none; }

  .input-hint-row {
    display: flex;
    justify-content: space-between;
    padding: 0 4px;
    font-size: 11px;
    color: var(--text-faint);
  }

  /* Modals */
  .modal-overlay {
    display: none;
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0,0,0,0.65);
    backdrop-filter: blur(4px);
    z-index: 100;
    align-items: center;
    justify-content: center;
    padding: 20px;
  }
  .modal-overlay.open { display: flex; }
  .modal-card {
    background: var(--bg-surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-xl);
    max-width: 560px;
    width: 100%;
    padding: 26px;
    color: var(--text-main);
    box-shadow: var(--shadow-lg);
    animation: scaleIn 0.18s ease;
  }
  @keyframes scaleIn { from { transform: scale(0.96); opacity: 0; } to { transform: scale(1); opacity: 1; } }
  .modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
  }
  .modal-header h3 { font-size: 18px; font-weight: 700; }
  .modal-close-icon {
    background: none;
    border: none;
    color: var(--text-muted);
    font-size: 18px;
    cursor: pointer;
    padding: 4px;
    border-radius: 6px;
  }
  .modal-close-icon:hover { color: var(--text-main); }
  .code-chip {
    background: var(--code-bg);
    border: 1px solid var(--border);
    padding: 10px 14px;
    border-radius: var(--radius-md);
    font-family: var(--font-mono);
    font-size: 13px;
    color: var(--accent);
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    word-break: break-all;
  }
  .modal-btn {
    width: 100%;
    background: var(--accent-gradient);
    color: #fff;
    border: none;
    padding: 11px;
    border-radius: var(--radius-md);
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    margin-top: 10px;
    transition: all 0.15s;
  }
  .modal-btn:hover { filter: brightness(1.08); }

  /* Log Table Styling */
  .log-table-container {
    max-height: 360px;
    overflow-y: auto;
    background: var(--code-bg);
    border: 1px solid var(--border);
    border-radius: var(--radius-md);
    font-family: var(--font-mono);
    font-size: 12px;
  }
  table.log-table {
    width: 100%;
    border-collapse: collapse;
    text-align: left;
  }
  table.log-table th, table.log-table td {
    padding: 9px 12px;
    border-bottom: 1px solid var(--border);
  }
  table.log-table th {
    background: var(--bg-card);
    color: var(--text-muted);
    position: sticky;
    top: 0;
  }
  .status-badge {
    display: inline-block;
    padding: 2px 7px;
    border-radius: 5px;
    font-size: 11px;
    font-weight: 700;
  }
  .status-200 { background: rgba(16, 185, 129, 0.18); color: #10b981; }
  .status-400 { background: rgba(245, 158, 11, 0.18); color: #f59e0b; }
  .status-401 { background: rgba(239, 68, 68, 0.18); color: #ef4444; }
  .status-500 { background: rgba(239, 68, 68, 0.28); color: #ef4444; }

  /* Passcode Modal */
  #passcodeOverlay {
    position: fixed;
    inset: 0;
    background: rgba(11, 13, 19, 0.95);
    backdrop-filter: blur(12px);
    z-index: 999;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
  }
  .passcode-card {
    background: var(--bg-surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-xl);
    max-width: 380px;
    width: 100%;
    padding: 34px 28px;
    text-align: center;
    box-shadow: var(--shadow-lg);
  }
  .passcode-icon {
    width: 56px;
    height: 56px;
    background: var(--accent-soft);
    color: var(--accent);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26px;
    margin: 0 auto 16px;
  }
  .passcode-input {
    width: 100%;
    padding: 12px 16px;
    font-size: 20px;
    text-align: center;
    letter-spacing: 6px;
    background: var(--bg-card);
    border: 1.5px solid var(--border);
    color: var(--text-main);
    border-radius: var(--radius-md);
    outline: none;
    margin-bottom: 16px;
    font-family: var(--font-mono);
  }
  .passcode-input:focus { border-color: var(--accent); }

  /* Floating Toast */
  #toast {
    position: fixed;
    bottom: 24px;
    left: 50%;
    transform: translateX(-50%) translateY(30px);
    background: #1f2937;
    color: #fff;
    padding: 9px 18px;
    border-radius: 20px;
    font-size: 13px;
    font-weight: 500;
    box-shadow: var(--shadow-lg);
    opacity: 0;
    pointer-events: none;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
    z-index: 120;
    border: 1px solid rgba(255,255,255,0.1);
  }
  #toast.show {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }

  /* Responsive Mobile */
  @media (max-width: 768px) {
    #sidebar {
      position: absolute;
      top: 0; bottom: 0; left: 0;
      transform: translateX(-100%);
      box-shadow: var(--shadow-lg);
    }
    #sidebar.open { transform: translateX(0); margin-left: 0; }
    .bubble { max-width: 100%; }
    .chat-messages { padding: 16px 12px; }
    .chat-input-container { padding: 10px 12px 16px; }
    .chat-header { padding: 0 12px; }
    .starter-cards { grid-template-columns: 1fr; }
    .hide-on-mobile { display: none; }
    select.model-select { max-width: 130px; font-size: 12px; }
  }
</style>
</head>
<body data-theme="dark">

<!-- Passcode Protection Modal -->
<div id="passcodeOverlay" style="display: ${if (isPasscodeProtected) "flex" else "none"};">
  <div class="passcode-card">
    <div class="passcode-icon">🔒</div>
    <h3 style="margin-bottom:8px;">Access Restricted</h3>
    <p style="font-size:13px; color:var(--text-muted); margin-bottom:20px; line-height:1.5;">
      This phone server is protected by a PIN passcode. Enter the code shown in the OpenLLM Android app to access.
    </p>
    <input type="password" id="passcodeInput" class="passcode-input" placeholder="••••" maxlength="16" autofocus onkeydown="if(event.key==='Enter')verifyPasscode()">
    <button class="modal-btn" onclick="verifyPasscode()" style="margin-top:0;">Unlock Gateway</button>
    <div id="passcodeError" style="color:#ef4444; font-size:12px; margin-top:12px; display:none;">Incorrect passcode. Please check your phone.</div>
  </div>
</div>

<!-- Hidden File Input for Attachments -->
<input type="file" id="fileUploadInput" style="display:none;" onchange="handleFileSelected(event)" accept=".txt,.json,.md,.csv,.py,.js,.kt,.java,.html,.css,.xml,image/*">

<!-- Sidebar Overlay for Mobile -->
<div id="sidebarOverlay" style="display:none; position:fixed; inset:0; background:rgba(0,0,0,0.5); z-index:40;" onclick="toggleSidebar()"></div>

<!-- Sidebar -->
<div id="sidebar">
  <div class="sidebar-header">
    <div class="brand">
      <div class="brand-logo-icon">⚡</div>
      <span>OpenLLM Gateway</span>
    </div>
    <button class="sidebar-toggle-btn" onclick="toggleSidebar()" title="Hide Sidebar">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6L6 18M6 6l12 12"/></svg>
    </button>
  </div>

  <div class="sidebar-action-wrap">
    <button class="new-chat-btn" onclick="createNewChat()">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
      New Chat
    </button>
    <div class="search-box">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
      <input type="text" id="sessionSearch" placeholder="Search chats..." oninput="filterSessions(this.value)">
    </div>
  </div>

  <div class="sessions-header">
    <span>Conversations</span>
    <span id="sessionCount">0</span>
  </div>

  <div class="sessions-list" id="sessionsList">
    <!-- Populated by JS -->
  </div>

  <div class="sidebar-footer">
    <div class="host-status">
      <div class="pulse-dot"></div>
      <span id="uptime">Online</span>
    </div>
    <span style="font-size:11px; color:var(--text-faint);">$deviceIp:$port</span>
  </div>
</div>

<!-- Main Workspace -->
<div id="main">
  <div class="chat-header">
    <div class="header-left">
      <button class="nav-btn" onclick="toggleSidebar()" title="Toggle Sidebar">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>
      </button>

      <div class="model-pill-container">
        <span class="model-chip-tag">AI Model</span>
        <select class="model-select" id="modelSelect" onchange="onModelChange()">
          <option value="">Loading models...</option>
        </select>
      </div>
    </div>

    <div class="header-right">
      <button class="header-action-btn" onclick="openExportModal()" title="Export Chat">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
        <span class="hide-on-mobile">Export</span>
      </button>

      <button class="header-action-btn" onclick="openLogsModal()" title="Live Server Logs">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
        <span class="hide-on-mobile">Logs</span>
      </button>

      <button class="header-action-btn" onclick="openApiModal()" title="Connect Third-Party Apps">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg>
        <span class="hide-on-mobile">API</span>
      </button>

      <button class="header-action-btn icon-only" onclick="toggleTheme()" id="themeBtn" title="Toggle Theme">
        ☀️
      </button>
    </div>
  </div>

  <div class="chat-messages" id="chatMessages">
    <div class="messages-flow" id="messagesFlow">
      <div class="welcome-banner" id="welcomeBanner">
        <div class="welcome-badge">
          <span>● Hosted on Android Phone</span>
          <span>•</span>
          <span>$deviceIp:$port</span>
        </div>
        <h2>OpenLLM Web Gateway</h2>
        <p>Chat directly with your phone's active AI models, upload files, query real-time live web search results, or export conversations.</p>

        <div class="starter-cards">
          <div class="starter-card" onclick="sendQuickPrompt('Explain quantum computing and quantum superposition with intuitive analogies.')">
            <span class="icon">⚛️</span>
            <div class="title">Explain Quantum Computing</div>
            <div class="desc">Intuitive breakdown with real-world analogies</div>
          </div>
          <div class="starter-card" onclick="sendQuickPrompt('What are the latest developments in AI technology this week?')">
            <span class="icon">🌐</span>
            <div class="title">Live Web Search Query</div>
            <div class="desc">Uses real-time search engine results</div>
          </div>
          <div class="starter-card" onclick="sendQuickPrompt('Write a clean, production-ready REST API client in Python using requests or httpx.')">
            <span class="icon">💻</span>
            <div class="title">Write Python REST Client</div>
            <div class="desc">Clean code with typing and error handling</div>
          </div>
          <div class="starter-card" onclick="sendQuickPrompt('Give me 5 high-impact daily habits for software engineers to avoid burnout.')">
            <span class="icon">🚀</span>
            <div class="title">Engineering Productivity</div>
            <div class="desc">5 actionable habits to stay sharp and balanced</div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="chat-input-container">
    <div class="input-wrapper">
      <div class="attachment-preview-bar" id="attachmentPreviewBar">
        <div class="attachment-info">
          <span id="attachmentIcon">📎</span>
          <span id="attachmentFileName" style="font-weight:600;">file.txt</span>
          <span id="attachmentFileSize" style="color:var(--text-faint); font-size:11px;">(12 KB)</span>
        </div>
        <button class="attachment-remove-btn" onclick="clearAttachment()" title="Remove file">✕</button>
      </div>

      <div class="input-toolbar">
        <label class="web-search-chip active" id="webSearchChip">
          <input type="checkbox" id="webSearchToggle" checked onchange="updateWebSearchChip()">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="2" y1="12" x2="22" y2="12"></line><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path></svg>
          <span id="webSearchLabel">Web Search Active</span>
        </label>
        <span id="statusIndicator" style="font-size:11.5px; color:var(--text-muted);">Connected to Phone</span>
      </div>

      <div class="input-box" id="dropZone">
        <button class="attach-btn" onclick="document.getElementById('fileUploadInput').click()" title="Attach document or image (Max 5MB)">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"></path></svg>
        </button>
        <textarea id="userInput" rows="1" placeholder="Type a message or drag & drop files... (Enter to send, Shift+Enter for newline)" onkeydown="handleKeyDown(event)" oninput="autoGrow(this)" onpaste="handlePaste(event)"></textarea>
        <button id="sendBtn" onclick="sendMessage()" title="Send message">
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"></line><polygon points="22 2 15 22 11 13 2 9 22 2"></polygon></svg>
        </button>
      </div>

      <div class="input-hint-row">
        <span>Press <kbd style="background:var(--bg-card); padding:1px 4px; border-radius:3px;">Enter</kbd> to send, <kbd style="background:var(--bg-card); padding:1px 4px; border-radius:3px;">Shift + Enter</kbd> for newline</span>
        <span>Drag & drop files or paste images</span>
      </div>
    </div>
  </div>
</div>

<!-- Floating Toast -->
<div id="toast"></div>

<!-- API Setup Modal -->
<div class="modal-overlay" id="apiModal">
  <div class="modal-card">
    <div class="modal-header">
      <h3>⚡ Connect Third-Party Apps</h3>
      <button class="modal-close-icon" onclick="closeApiModal()">✕</button>
    </div>
    <p style="font-size:13px; color:var(--text-muted); line-height:1.5; margin-bottom:18px;">
      Your phone exposes an OpenAI-compatible HTTP API. You can connect tools like <b>Open WebUI</b>, <b>LibreChat</b>, <b>Obsidian</b>, <b>Cursor</b>, or custom scripts!
    </p>

    <div style="font-size:12px; font-weight:600; margin-bottom:6px;">OpenAI Base URL:</div>
    <div class="code-chip">
      <span id="baseUrlText">http://$deviceIp:$port/v1</span>
      <button class="header-action-btn" style="padding:3px 8px; font-size:11px;" onclick="copyText('baseUrlText')">Copy</button>
    </div>

    <div style="font-size:12px; font-weight:600; margin-bottom:6px;">API Key:</div>
    <div class="code-chip">
      <span id="apiKeyText">sk-local-openllm</span>
      <button class="header-action-btn" style="padding:3px 8px; font-size:11px;" onclick="copyText('apiKeyText')">Copy</button>
    </div>

    <div style="font-size:12px; font-weight:600; margin-bottom:6px;">Quick cURL Test:</div>
    <div class="code-chip" style="font-size:11px; color:var(--text-muted);">
      <span id="curlExample">curl http://$deviceIp:$port/v1/models</span>
      <button class="header-action-btn" style="padding:3px 8px; font-size:11px;" onclick="copyText('curlExample')">Copy</button>
    </div>

    <button class="modal-btn" onclick="closeApiModal()">Done</button>
  </div>
</div>

<!-- Export Modal -->
<div class="modal-overlay" id="exportModal">
  <div class="modal-card">
    <div class="modal-header">
      <h3>📥 Export Chat Conversation</h3>
      <button class="modal-close-icon" onclick="closeExportModal()">✕</button>
    </div>
    <p style="font-size:13px; color:var(--text-muted); line-height:1.5; margin-bottom:18px;">
      Download the entire conversation history of the active session formatted for notes or data processing.
    </p>
    <div style="display:grid; grid-template-columns: 1fr 1fr; gap:12px; margin-bottom:18px;">
      <button class="starter-card" style="text-align:center; padding:16px;" onclick="downloadExport('markdown')">
        <span class="icon">📄</span>
        <div class="title">Markdown (.md)</div>
        <div class="desc">Formatted for Obsidian, Notion, or GitHub</div>
      </button>
      <button class="starter-card" style="text-align:center; padding:16px;" onclick="downloadExport('json')">
        <span class="icon">📦</span>
        <div class="title">JSON (.json)</div>
        <div class="desc">Full structured data with metadata</div>
      </button>
    </div>
    <button class="modal-btn" style="background:var(--bg-card); color:var(--text-main); border:1px solid var(--border);" onclick="closeExportModal()">Cancel</button>
  </div>
</div>

<!-- Live Server Logs Modal -->
<div class="modal-overlay" id="logsModal">
  <div class="modal-card" style="max-width: 740px;">
    <div class="modal-header">
      <div style="display:flex; align-items:center; gap:8px;">
        <h3>📊 Live Server Request Inspector</h3>
        <span class="status-badge status-200">Live</span>
      </div>
      <div style="display:flex; gap:6px;">
        <button class="header-action-btn" onclick="fetchServerLogs()" style="font-size:11.5px; padding:4px 8px;">🔄 Refresh</button>
        <button class="header-action-btn" onclick="clearServerLogs()" style="font-size:11.5px; padding:4px 8px; color:#ef4444;">Clear</button>
        <button class="modal-close-icon" onclick="closeLogsModal()">✕</button>
      </div>
    </div>
    <p style="font-size:12.5px; color:var(--text-muted); margin-bottom:12px;">
      Every HTTP request handled by your Android device's server in real-time.
    </p>
    <div class="log-table-container">
      <table class="log-table">
        <thead>
          <tr>
            <th>Time</th>
            <th>Method</th>
            <th>Path</th>
            <th>Status</th>
            <th>Duration</th>
            <th>Client</th>
            <th>Detail</th>
          </tr>
        </thead>
        <tbody id="logsTableBody">
          <tr><td colspan="7" style="text-align:center; color:var(--text-muted); padding:24px;">Loading live server logs...</td></tr>
        </tbody>
      </table>
    </div>
    <button class="modal-btn" onclick="closeLogsModal()" style="margin-top:16px;">Close Inspector</button>
  </div>
</div>

<script>
let currentSessionId = null;
let isGenerating = false;
let storedPasscode = localStorage.getItem('openllm_passcode') || '';
let activeAttachment = null;
let allSessions = [];

function getHeaders(customHeaders = {}) {
  const headers = { ...customHeaders };
  if (storedPasscode) {
    headers['X-Passcode'] = storedPasscode;
  }
  return headers;
}

// Passcode Auth
async function verifyPasscode() {
  const input = document.getElementById('passcodeInput');
  const errorEl = document.getElementById('passcodeError');
  const pin = input.value.trim();
  if (!pin) return;

  try {
    const res = await fetch('/api/auth/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ passcode: pin })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      storedPasscode = pin;
      localStorage.setItem('openllm_passcode', pin);
      document.getElementById('passcodeOverlay').style.display = 'none';
      errorEl.style.display = 'none';
      showToast('Gateway Unlocked');
      initApp();
    } else {
      errorEl.style.display = 'block';
      errorEl.innerText = data.error || 'Incorrect passcode';
    }
  } catch (e) {
    errorEl.style.display = 'block';
    errorEl.innerText = 'Connection error: ' + e.message;
  }
}

// File and Image Attachments
function handleFileSelected(event) {
  const file = event.target.files[0];
  if (!file) return;
  processUploadedFile(file);
}

function processUploadedFile(file) {
  if (file.size > 5 * 1024 * 1024) {
    showToast('File size must be under 5MB');
    return;
  }
  const isImage = file.type.startsWith('image/');
  const reader = new FileReader();

  if (isImage) {
    reader.onload = function(e) {
      activeAttachment = {
        name: file.name,
        type: file.type,
        size: file.size,
        text: null,
        base64: e.target.result
      };
      displayAttachmentPreview();
    };
    reader.readAsDataURL(file);
  } else {
    reader.onload = function(e) {
      activeAttachment = {
        name: file.name,
        type: file.type || 'text/plain',
        size: file.size,
        text: e.target.result,
        base64: null
      };
      displayAttachmentPreview();
    };
    reader.readAsText(file);
  }
}

function displayAttachmentPreview() {
  if (!activeAttachment) return;
  const bar = document.getElementById('attachmentPreviewBar');
  const nameEl = document.getElementById('attachmentFileName');
  const sizeEl = document.getElementById('attachmentFileSize');
  const iconEl = document.getElementById('attachmentIcon');

  nameEl.innerText = activeAttachment.name;
  sizeEl.innerText = '(' + formatBytes(activeAttachment.size) + ')';
  iconEl.innerText = activeAttachment.type.startsWith('image/') ? '🖼️' : '📄';
  bar.classList.add('active');
  showToast('Attached: ' + activeAttachment.name);
}

function clearAttachment() {
  activeAttachment = null;
  document.getElementById('fileUploadInput').value = '';
  document.getElementById('attachmentPreviewBar').classList.remove('active');
}

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1048576).toFixed(1) + ' MB';
}

// Drag and drop into input box
const dropZone = document.getElementById('dropZone');
['dragenter', 'dragover'].forEach(name => {
  dropZone.addEventListener(name, (e) => {
    e.preventDefault();
    dropZone.style.borderColor = 'var(--accent)';
  }, false);
});
['dragleave', 'drop'].forEach(name => {
  dropZone.addEventListener(name, (e) => {
    e.preventDefault();
    dropZone.style.borderColor = '';
  }, false);
});
dropZone.addEventListener('drop', (e) => {
  const dt = e.dataTransfer;
  const files = dt.files;
  if (files.length > 0) {
    processUploadedFile(files[0]);
  }
});

// Clipboard paste image support
function handlePaste(e) {
  const items = (e.clipboardData || window.clipboardData).items;
  for (let i = 0; i < items.length; i++) {
    if (items[i].type.indexOf('image') !== -1) {
      const blob = items[i].getAsFile();
      const reader = new FileReader();
      reader.onload = function(event) {
        activeAttachment = {
          name: 'pasted_screenshot.png',
          type: 'image/png',
          size: blob.size,
          text: null,
          base64: event.target.result
        };
        displayAttachmentPreview();
      };
      reader.readAsDataURL(blob);
    }
  }
}

// Web Search Chip
function updateWebSearchChip() {
  const toggle = document.getElementById('webSearchToggle');
  const chip = document.getElementById('webSearchChip');
  const label = document.getElementById('webSearchLabel');
  if (toggle.checked) {
    chip.classList.add('active');
    label.innerText = 'Web Search Active';
  } else {
    chip.classList.remove('active');
    label.innerText = 'Web Search Off';
  }
}

// Export Chat
function openExportModal() {
  if (!currentSessionId) {
    showToast('Select or start a chat first');
    return;
  }
  document.getElementById('exportModal').classList.add('open');
}
function closeExportModal() {
  document.getElementById('exportModal').classList.remove('open');
}
function downloadExport(format) {
  if (!currentSessionId) return;
  const url = '/api/export?sessionId=' + currentSessionId + '&format=' + format;
  const a = document.createElement('a');
  a.href = url;
  a.download = 'chat_export_' + currentSessionId + '.' + (format === 'json' ? 'json' : 'md');
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  closeExportModal();
  showToast('Export downloaded');
}

// Live Server Logs
async function openLogsModal() {
  document.getElementById('logsModal').classList.add('open');
  await fetchServerLogs();
}
function closeLogsModal() {
  document.getElementById('logsModal').classList.remove('open');
}
async function fetchServerLogs() {
  try {
    const res = await fetch('/api/logs', { headers: getHeaders() });
    if (!res.ok) return;
    const data = await res.json();
    const tbody = document.getElementById('logsTableBody');
    tbody.innerHTML = '';
    if (!data.logs || data.logs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding:20px; color:var(--text-muted);">No requests logged yet.</td></tr>';
      return;
    }
    data.logs.forEach(log => {
      const d = new Date(log.timestamp);
      const timeStr = d.toLocaleTimeString();
      const statusClass = log.statusCode < 300 ? 'status-200' : (log.statusCode < 500 ? 'status-400' : 'status-500');
      const tr = document.createElement('tr');
      tr.innerHTML = 
        '<td>' + timeStr + '</td>' +
        '<td style="font-weight:700;">' + escapeHtml(log.method) + '</td>' +
        '<td>' + escapeHtml(log.path) + '</td>' +
        '<td><span class="status-badge ' + statusClass + '">' + log.statusCode + '</span></td>' +
        '<td>' + log.durationMs + 'ms</td>' +
        '<td style="color:var(--text-muted);">' + escapeHtml(log.clientIp) + '</td>' +
        '<td style="color:var(--text-muted);">' + escapeHtml(log.detail || '') + '</td>';
      tbody.appendChild(tr);
    });
  } catch (e) {
    showToast('Failed to fetch logs: ' + e);
  }
}
async function clearServerLogs() {
  try {
    await fetch('/api/logs/clear', { method: 'POST', headers: getHeaders() });
    fetchServerLogs();
    showToast('Logs cleared');
  } catch (e) {
    showToast('Error: ' + e);
  }
}

// App Initialization
async function initApp() {
  await checkStatus();
  await loadModels();
  await loadSessions();
}

window.onload = () => {
  const isProtected = ${isPasscodeProtected};
  if (isProtected) {
    if (storedPasscode) {
      document.getElementById('passcodeInput').value = storedPasscode;
      verifyPasscode();
    }
  } else {
    initApp();
  }
};

// Check Status
async function checkStatus() {
  try {
    const res = await fetch('/api/status', { headers: getHeaders() });
    if (res.ok) {
      document.getElementById('uptime').innerText = 'Online';
      document.getElementById('statusIndicator').innerText = 'Connected to Phone';
    }
  } catch (e) {
    document.getElementById('uptime').innerText = 'Offline';
    document.getElementById('statusIndicator').innerText = 'Disconnected';
  }
}

// Load Models
async function loadModels() {
  try {
    const res = await fetch('/api/models', { headers: getHeaders() });
    if (!res.ok) return;
    const data = await res.json();
    const select = document.getElementById('modelSelect');
    select.innerHTML = '';
    data.models.forEach(m => {
      const opt = document.createElement('option');
      opt.value = m.id;
      opt.text = m.modelName;
      if (m.isActive) opt.selected = true;
      select.appendChild(opt);
    });
  } catch (e) {
    console.error('Failed to load models', e);
  }
}

async function onModelChange() {
  const select = document.getElementById('modelSelect');
  const selectedId = select.value;
  if (!selectedId) return;

  try {
    await fetch('/api/models/select', {
      method: 'POST',
      headers: getHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({ id: parseInt(selectedId) })
    });
    showToast('Model switched to ' + select.options[select.selectedIndex].text);
  } catch (e) {
    showToast('Failed to switch model: ' + e);
  }
}

// Sessions Management
async function loadSessions() {
  try {
    const res = await fetch('/api/sessions', { headers: getHeaders() });
    if (!res.ok) return;
    const data = await res.json();
    allSessions = data.sessions || [];
    renderSessions(allSessions);
    if (!currentSessionId && allSessions.length > 0) {
      selectSession(allSessions[0].id);
    }
  } catch (e) {
    console.error('Failed to load sessions', e);
  }
}

function renderSessions(sessions) {
  const list = document.getElementById('sessionsList');
  const countEl = document.getElementById('sessionCount');
  countEl.innerText = sessions.length;
  list.innerHTML = '';

  if (sessions.length === 0) {
    list.innerHTML = '<div style="padding:16px; text-align:center; color:var(--text-faint); font-size:12px;">No chats yet</div>';
    return;
  }

  sessions.forEach(s => {
    const item = document.createElement('div');
    item.className = 'session-item' + (s.id === currentSessionId ? ' active' : '');
    item.onclick = () => selectSession(s.id);
    item.innerHTML = 
      '<span class="session-icon">💬</span>' +
      '<span class="session-title">' + escapeHtml(s.title || 'Chat') + '</span>' +
      '<button class="session-delete" title="Delete chat" onclick="deleteSession(event, ' + s.id + ')">✕</button>';
    list.appendChild(item);
  });
}

function filterSessions(query) {
  const q = query.trim().toLowerCase();
  if (!q) {
    renderSessions(allSessions);
    return;
  }
  const filtered = allSessions.filter(s => (s.title || '').toLowerCase().includes(q));
  renderSessions(filtered);
}

async function selectSession(id) {
  currentSessionId = id;
  document.querySelectorAll('.session-item').forEach(el => el.classList.remove('active'));
  renderSessions(allSessions);
  loadMessages(id);
  if (window.innerWidth <= 768) {
    const sidebar = document.getElementById('sidebar');
    if (sidebar.classList.contains('open')) toggleSidebar();
  }
}

async function createNewChat() {
  currentSessionId = null;
  clearAttachment();
  const flow = document.getElementById('messagesFlow');
  flow.innerHTML = 
    '<div class="welcome-banner" id="welcomeBanner">' +
      '<div class="welcome-badge"><span>● Ready</span><span>•</span><span>New Conversation</span></div>' +
      '<h2>New Chat Session</h2>' +
      '<p>Ask a question, upload a document or code file, or try one of the prompt starters below.</p>' +
      '<div class="starter-cards">' +
        '<div class="starter-card" onclick="sendQuickPrompt(\'Explain quantum computing and quantum superposition with intuitive analogies.\')">' +
          '<span class="icon">⚛️</span>' +
          '<div class="title">Explain Quantum Computing</div>' +
          '<div class="desc">Intuitive breakdown with analogies</div>' +
        '</div>' +
        '<div class="starter-card" onclick="sendQuickPrompt(\'What are the latest developments in AI technology this week?\')">' +
          '<span class="icon">🌐</span>' +
          '<div class="title">Live Web Search Query</div>' +
          '<div class="desc">Real-time up to date answers</div>' +
        '</div>' +
      '</div>' +
    '</div>';
  document.getElementById('userInput').focus();
  if (window.innerWidth <= 768) {
    const sidebar = document.getElementById('sidebar');
    if (sidebar.classList.contains('open')) toggleSidebar();
  }
}

async function deleteSession(e, id) {
  e.stopPropagation();
  if (!confirm('Delete this conversation?')) return;
  try {
    await fetch('/api/sessions?id=' + id, { method: 'DELETE', headers: getHeaders() });
    if (currentSessionId === id) currentSessionId = null;
    loadSessions();
    if (!currentSessionId) createNewChat();
  } catch (err) {
    showToast('Failed to delete: ' + err);
  }
}

async function loadMessages(sessionId) {
  try {
    const res = await fetch('/api/messages?sessionId=' + sessionId, { headers: getHeaders() });
    if (!res.ok) return;
    const data = await res.json();
    const flow = document.getElementById('messagesFlow');
    flow.innerHTML = '';
    if (data.messages.length === 0) {
      createNewChat();
      return;
    }
    data.messages.forEach(m => {
      appendMessageUI(m.role, m.content, m.attachmentName, m.imageUri);
    });
    const container = document.getElementById('chatMessages');
    container.scrollTop = container.scrollHeight;
  } catch (e) {
    console.error(e);
  }
}

function sendQuickPrompt(prompt) {
  const el = document.getElementById('userInput');
  el.value = prompt;
  autoGrow(el);
  sendMessage();
}

// Chat Sending
async function sendMessage() {
  const input = document.getElementById('userInput');
  const text = input.value.trim();
  if ((!text && !activeAttachment) || isGenerating) return;

  const webSearch = document.getElementById('webSearchToggle').checked;
  const welcome = document.getElementById('welcomeBanner');
  if (welcome) welcome.remove();

  const currentAttach = activeAttachment;
  appendMessageUI('user', text, currentAttach ? currentAttach.name : null, currentAttach ? currentAttach.base64 : null);
  input.value = '';
  input.style.height = 'auto';
  clearAttachment();

  isGenerating = true;
  document.getElementById('sendBtn').disabled = true;
  document.getElementById('statusIndicator').innerText = 'Generating answer...';

  const typingId = appendTypingIndicator();

  try {
    const payload = {
      message: text,
      sessionId: currentSessionId,
      webSearch: webSearch
    };

    if (currentAttach) {
      payload.attachmentName = currentAttach.name;
      payload.attachmentType = currentAttach.type;
      payload.attachmentText = currentAttach.text;
      payload.imageBase64 = currentAttach.base64;
    }

    const res = await fetch('/api/chat', {
      method: 'POST',
      headers: getHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(payload)
    });

    removeElement(typingId);

    if (!res.ok) {
      const err = await res.text();
      appendMessageUI('assistant', '⚠️ Error: ' + err);
      return;
    }

    const data = await res.json();
    if (data.sessionId && !currentSessionId) {
      currentSessionId = data.sessionId;
      loadSessions();
    }
    appendMessageUI('assistant', data.content || '(Empty reply)');
  } catch (err) {
    removeElement(typingId);
    appendMessageUI('assistant', '⚠️ Network Error: ' + err.message);
  } finally {
    isGenerating = false;
    document.getElementById('sendBtn').disabled = false;
    document.getElementById('statusIndicator').innerText = 'Connected to Phone';
  }
}

// Rich Message Rendering
function appendMessageUI(role, content, attachmentName = null, imageUri = null) {
  const flow = document.getElementById('messagesFlow');
  const row = document.createElement('div');
  row.className = 'message-row ' + role;

  const isUser = role === 'user';
  const avatarHtml = isUser 
    ? '<div class="avatar user-avatar">U</div>'
    : '<div class="avatar ai-avatar">AI</div>';

  let extraHtml = '';
  if (attachmentName) {
    extraHtml += '<div class="bubble-attachment">📎 ' + escapeHtml(attachmentName) + '</div>';
  }
  if (imageUri && imageUri.startsWith('data:image')) {
    extraHtml += '<img src="' + imageUri + '" style="max-width:100%; border-radius:10px; margin-bottom:8px; display:block;" />';
  }

  const formattedContent = isUser ? escapeHtml(content) : parseRichMarkdown(content);

  // Message Actions for Assistant (Copy & TTS)
  let actionsHtml = '';
  if (!isUser) {
    actionsHtml = 
      '<div class="message-actions">' +
        '<button class="msg-act-btn" onclick="copyMessageText(this)" title="Copy message">' +
          '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>' +
          'Copy' +
        '</button>' +
        '<button class="msg-act-btn" onclick="speakMessage(this)" title="Read aloud">' +
          '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path></svg>' +
          'Speak' +
        '</button>' +
      '</div>';
  }

  row.innerHTML = isUser
    ? '<div class="message-body-wrap"><div class="bubble">' + extraHtml + formattedContent + '</div></div>' + avatarHtml
    : avatarHtml + '<div class="message-body-wrap"><div class="bubble" data-raw="' + encodeURIComponent(content) + '">' + extraHtml + formattedContent + '</div>' + actionsHtml + '</div>';

  flow.appendChild(row);
  const container = document.getElementById('chatMessages');
  container.scrollTop = container.scrollHeight;
}

function copyMessageText(btn) {
  const wrap = btn.closest('.message-body-wrap');
  const bubble = wrap.querySelector('.bubble');
  const raw = bubble.getAttribute('data-raw');
  const text = raw ? decodeURIComponent(raw) : bubble.innerText;
  navigator.clipboard.writeText(text).then(() => {
    btn.innerHTML = '✓ Copied';
    showToast('Message copied to clipboard');
    setTimeout(() => {
      btn.innerHTML = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg> Copy';
    }, 2000);
  });
}

function speakMessage(btn) {
  if (!('speechSynthesis' in window)) {
    showToast('Text-to-speech not supported in this browser');
    return;
  }
  if (window.speechSynthesis.speaking) {
    window.speechSynthesis.cancel();
    btn.classList.remove('active');
    return;
  }
  const wrap = btn.closest('.message-body-wrap');
  const bubble = wrap.querySelector('.bubble');
  const raw = bubble.getAttribute('data-raw');
  const text = raw ? decodeURIComponent(raw) : bubble.innerText;
  const utter = new SpeechSynthesisUtterance(text);
  btn.classList.add('active');
  utter.onend = () => btn.classList.remove('active');
  utter.onerror = () => btn.classList.remove('active');
  window.speechSynthesis.speak(utter);
}

function appendTypingIndicator() {
  const flow = document.getElementById('messagesFlow');
  const id = 'typing-' + Date.now();
  const row = document.createElement('div');
  row.id = id;
  row.className = 'message-row assistant';
  row.innerHTML = 
    '<div class="avatar ai-avatar">AI</div>' +
    '<div class="message-body-wrap"><div class="bubble" style="display:flex; align-items:center; gap:8px; color:var(--text-muted); font-size:13.5px;">' +
      '<div class="pulse-dot" style="width:6px; height:6px;"></div> Thinking...' +
    '</div></div>';
  flow.appendChild(row);
  const container = document.getElementById('chatMessages');
  container.scrollTop = container.scrollHeight;
  return id;
}

function removeElement(id) {
  const el = document.getElementById(id);
  if (el) el.remove();
}

function escapeHtml(text) {
  return (text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// Full Markdown Parser
function parseRichMarkdown(text) {
  if (!text) return '';

  // Extract Code Blocks first
  const codeBlocks = [];
  let processed = text.replace(/```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g, (match, lang, code) => {
    const idx = codeBlocks.length;
    const cleanLang = (lang || 'code').trim().toLowerCase();
    const safeCode = escapeHtml(code);
    const html = 
      '<div class="code-block-container">' +
        '<div class="code-block-header">' +
          '<span>' + cleanLang + '</span>' +
          '<button class="copy-code-btn" onclick="copyCodeSnippet(this)">' +
            '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>' +
            'Copy' +
          '</button>' +
        '</div>' +
        '<pre><code>' + safeCode + '</code></pre>' +
      '</div>';
    codeBlocks.push(html);
    return '___CODE_BLOCK_' + idx + '___';
  });

  // Basic HTML escaping
  processed = escapeHtml(processed);

  // Markdown Headings
  processed = processed.replace(/^### (.*$)/gim, '<h3>$1</h3>');
  processed = processed.replace(/^## (.*$)/gim, '<h2>$1</h2>');
  processed = processed.replace(/^# (.*$)/gim, '<h1>$1</h1>');

  // Blockquotes
  processed = processed.replace(/^\> (.*$)/gim, '<blockquote>$1</blockquote>');

  // Bold & Italic
  processed = processed.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  processed = processed.replace(/\*([^*]+)\*/g, '<em>$1</em>');
  processed = processed.replace(/`([^`]+)`/g, '<code>$1</code>');

  // Horizontal Rule
  processed = processed.replace(/^---$/gim, '<hr>');

  // Lists (Ordered & Unordered)
  // Numbered list item: e.g. "1. Item" or "10. Item"
  processed = processed.replace(/^\s*(\d+)\.\s+(.*)$/gm, '<li data-num="$1">$2</li>');
  // Unordered list item: e.g. "- Item" or "* Item"
  processed = processed.replace(/^\s*[-*]\s+(.*)$/gm, '<li>$1</li>');

  // Wrap consecutive <li> into <ol> or <ul>
  processed = processed.replace(/(<li data-num="[^"]*">.*?<\/li>(?:\s*<li data-num="[^"]*">.*?<\/li>)*)/gs, '<ol>$1</ol>');
  processed = processed.replace(/(<li>.*?<\/li>(?:\s*<li>.*?<\/li>)*)/gs, '<ul>$1</ul>');

  // Convert remaining newlines into <br> (avoiding inside tags)
  processed = processed.replace(/\n{2,}/g, '</p><p>');
  processed = processed.replace(/\n/g, '<br>');
  processed = '<p>' + processed + '</p>';
  processed = processed.replace(/<p><\/(ol|ul|h1|h2|h3|blockquote|hr)>/g, '</$1>');
  processed = processed.replace(/<(ol|ul|h1|h2|h3|blockquote|hr)><p>/g, '<$1>');
  processed = processed.replace(/<p><\/p>/g, '');

  // Restore Code Blocks
  codeBlocks.forEach((blockHtml, i) => {
    processed = processed.replace('___CODE_BLOCK_' + i + '___', blockHtml);
  });

  return processed;
}

function copyCodeSnippet(btn) {
  const pre = btn.closest('.code-block-container').querySelector('pre code');
  navigator.clipboard.writeText(pre.innerText).then(() => {
    btn.innerHTML = '✓ Copied';
    showToast('Code copied to clipboard');
    setTimeout(() => {
      btn.innerHTML = '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg> Copy';
    }, 2000);
  });
}

function handleKeyDown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
}

function autoGrow(el) {
  el.style.height = 'auto';
  el.style.height = Math.min(el.scrollHeight, 180) + 'px';
}

function toggleTheme() {
  const current = document.body.getAttribute('data-theme');
  const next = current === 'dark' ? 'light' : 'dark';
  document.body.setAttribute('data-theme', next);
  document.getElementById('themeBtn').innerHTML = next === 'dark' ? '☀️' : '🌙';
}

function toggleSidebar() {
  const sidebar = document.getElementById('sidebar');
  const overlay = document.getElementById('sidebarOverlay');
  if (window.innerWidth <= 768) {
    const isOpen = sidebar.classList.toggle('open');
    overlay.style.display = isOpen ? 'block' : 'none';
  } else {
    sidebar.classList.toggle('collapsed');
  }
}

function openApiModal() {
  document.getElementById('apiModal').classList.add('open');
}
function closeApiModal() {
  document.getElementById('apiModal').classList.remove('open');
}

function copyText(elementId) {
  const text = document.getElementById(elementId).innerText;
  navigator.clipboard.writeText(text).then(() => {
    showToast('Copied to clipboard');
  });
}

function showToast(msg) {
  const toast = document.getElementById('toast');
  if (!toast) return;
  toast.innerText = msg;
  toast.classList.add('show');
  setTimeout(() => {
    toast.classList.remove('show');
  }, 2400);
}
</script>
</body>
</html>"""
    }
}
