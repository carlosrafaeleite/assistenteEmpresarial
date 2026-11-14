(function () {
  const currentScript = document.currentScript || document.querySelector('script[data-slug]');
  if (!currentScript) return;

  const slug = currentScript.getAttribute('data-slug');
  if (!slug) {
    console.error('[AI Widget] Atributo "data-slug" é obrigatório.');
    return;
  }

  const scriptSrc = currentScript.src;
  const apiBase = currentScript.getAttribute('data-api-base') || 
                  (scriptSrc ? new URL(scriptSrc).origin : window.location.origin);

  let conversaId = sessionStorage.getItem('ai_widget_conversa_' + slug) || null;
  let senhaAcesso = currentScript.getAttribute('data-senha') || sessionStorage.getItem('ai_widget_senha_' + slug) || null;
  let isSending = false;
  let config = {
    corPrimaria: '#2563EB',
    corSecundaria: '#1E40AF',
    assistenteNome: 'Suporte IA',
    mensagemBoasVindas: 'Olá! Como posso ajudar você hoje?',
    protegidoPorSenha: false
  };

  // Carrega configuração pública da empresa/assistente
  fetch(`${apiBase}/api/publico/chat/${slug}/config`)
    .then(res => res.ok ? res.json() : Promise.reject('Configuração indisponível'))
    .then(data => {
      config = { ...config, ...data };
      initWidget();
    })
    .catch(err => {
      console.warn('[AI Widget] Falha ao obter configurações remotas, inicializando com padrões:', err);
      initWidget();
    });

  function initWidget() {
    const style = document.createElement('style');
    style.innerHTML = `
      #ai-chat-widget-root {
        position: fixed;
        bottom: 24px;
        right: 24px;
        z-index: 999999;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      }
      #ai-chat-btn {
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background-color: ${config.corPrimaria};
        box-shadow: 0 4px 16px rgba(0,0,0,0.22);
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        border: none;
        color: #fff;
        transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.2s ease;
      }
      #ai-chat-btn:hover {
        transform: scale(1.08);
        box-shadow: 0 6px 20px rgba(0,0,0,0.3);
      }
      #ai-chat-box {
        display: none;
        position: fixed;
        bottom: 96px;
        right: 24px;
        width: 390px;
        max-width: calc(100vw - 32px);
        height: 580px;
        max-height: calc(100vh - 120px);
        background: #ffffff;
        border-radius: 20px;
        box-shadow: 0 12px 35px rgba(0,0,0,0.2);
        overflow: hidden;
        flex-direction: column;
        border: 1px solid rgba(0,0,0,0.06);
      }
      #ai-chat-header {
        background: ${config.corPrimaria};
        color: #ffffff;
        padding: 16px 20px;
        display: flex;
        align-items: center;
        justify-content: space-between;
      }
      #ai-chat-header-info {
        display: flex;
        flex-direction: column;
      }
      #ai-chat-header-title {
        font-weight: 700;
        font-size: 15px;
        letter-spacing: -0.2px;
      }
      #ai-chat-header-subtitle {
        font-size: 11px;
        opacity: 0.85;
      }
      #ai-chat-close {
        background: rgba(255,255,255,0.15);
        border: none;
        color: #ffffff;
        width: 28px;
        height: 28px;
        border-radius: 50%;
        font-size: 16px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 0.15s;
      }
      #ai-chat-close:hover {
        background: rgba(255,255,255,0.3);
      }
      #ai-chat-messages {
        flex: 1;
        padding: 16px;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 12px;
        background: #f8fafc;
      }
      .ai-msg {
        max-width: 84%;
        padding: 11px 15px;
        border-radius: 16px;
        font-size: 13.5px;
        line-height: 1.5;
        word-break: break-word;
        white-space: pre-wrap;
      }
      .ai-msg-bot {
        align-self: flex-start;
        background: #ffffff;
        color: #1e293b;
        border: 1px solid #e2e8f0;
        border-bottom-left-radius: 4px;
        box-shadow: 0 1px 3px rgba(0,0,0,0.04);
      }
      .ai-msg-user {
        align-self: flex-end;
        background: ${config.corPrimaria};
        color: #ffffff;
        border-bottom-right-radius: 4px;
      }
      .ai-msg-sources {
        margin-top: 8px;
        padding-top: 6px;
        border-top: 1px solid #f1f5f9;
        font-size: 11px;
        color: #64748b;
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
        align-items: center;
      }
      .ai-source-badge {
        background: #f1f5f9;
        color: #334155;
        padding: 2px 6px;
        border-radius: 4px;
        font-family: monospace;
        font-size: 10px;
      }
      .ai-tool-pill {
        align-self: flex-start;
        background: #fef3c7;
        color: #92400e;
        border: 1px solid #fde68a;
        padding: 4px 10px;
        border-radius: 12px;
        font-size: 11.5px;
        display: flex;
        align-items: center;
        gap: 6px;
      }
      .ai-typing {
        font-size: 12px;
        color: #64748b;
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 4px 8px;
      }
      .ai-dot {
        width: 6px;
        height: 6px;
        background: #94a3b8;
        border-radius: 50%;
        animation: aiBounce 1.4s infinite ease-in-out both;
      }
      .ai-dot:nth-child(1) { animation-delay: -0.32s; }
      .ai-dot:nth-child(2) { animation-delay: -0.16s; }
      @keyframes aiBounce {
        0%, 80%, 100% { transform: scale(0); }
        40% { transform: scale(1); }
      }
      #ai-chat-input-bar {
        display: flex;
        padding: 12px 14px;
        border-top: 1px solid #e2e8f0;
        background: #ffffff;
        gap: 8px;
        align-items: center;
      }
      #ai-chat-input {
        flex: 1;
        border: 1px solid #cbd5e1;
        border-radius: 22px;
        padding: 10px 16px;
        outline: none;
        font-size: 13.5px;
        transition: border-color 0.15s, box-shadow 0.15s;
        background: #f8fafc;
      }
      #ai-chat-input:focus {
        border-color: ${config.corPrimaria};
        background: #ffffff;
        box-shadow: 0 0 0 3px rgba(37,99,235,0.12);
      }
      #ai-chat-send {
        background: ${config.corPrimaria};
        color: #fff;
        border: none;
        border-radius: 50%;
        width: 38px;
        height: 38px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: opacity 0.15s;
      }
      #ai-chat-send:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
      #ai-chat-footer {
        padding: 4px 12px 8px;
        text-align: center;
        background: #ffffff;
        font-size: 10px;
        color: #94a3b8;
        font-family: monospace;
      }
    `;
    document.head.appendChild(style);

    const root = document.createElement('div');
    root.id = 'ai-chat-widget-root';
    root.innerHTML = `
      <div id="ai-chat-box">
        <div id="ai-chat-header">
          <div id="ai-chat-header-info">
            <span id="ai-chat-header-title">${config.assistenteNome}</span>
            <span id="ai-chat-header-subtitle">Online • Streaming & RAG Ativos</span>
          </div>
          <button id="ai-chat-close">&times;</button>
        </div>
        <div id="ai-chat-messages">
          <div class="ai-msg ai-msg-bot">${config.mensagemBoasVindas}</div>
        </div>
        <div id="ai-chat-input-bar">
          <input type="text" id="ai-chat-input" placeholder="Digite sua mensagem..." autocomplete="off"/>
          <button id="ai-chat-send" aria-label="Enviar">&#10148;</button>
        </div>
        <div id="ai-chat-footer">🛡️ Guardrails PII • Streaming SSE</div>
      </div>
      <button id="ai-chat-btn" aria-label="Abrir Chat Inteligente">
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
        </svg>
      </button>
    `;
    document.body.appendChild(root);

    const box = document.getElementById('ai-chat-box');
    const btn = document.getElementById('ai-chat-btn');
    const close = document.getElementById('ai-chat-close');
    const input = document.getElementById('ai-chat-input');
    const send = document.getElementById('ai-chat-send');
    const messages = document.getElementById('ai-chat-messages');

    btn.onclick = () => {
      const isVisible = box.style.display === 'flex';
      box.style.display = isVisible ? 'none' : 'flex';
      if (!isVisible) input.focus();
    };

    close.onclick = () => {
      box.style.display = 'none';
    };

    async function handleSend() {
      const text = input.value.trim();
      if (!text || isSending) return;

      if (config.protegidoPorSenha && !senhaAcesso) {
        senhaAcesso = window.prompt('Este assistente é confidencial e requer uma senha de acesso:');
        if (senhaAcesso) {
          sessionStorage.setItem('ai_widget_senha_' + slug, senhaAcesso);
        } else {
          return;
        }
      }

      input.value = '';
      isSending = true;
      send.disabled = true;

      // Adiciona mensagem do usuário
      appendMessage('user', text);

      // Adiciona elemento temporário de digitação
      const typingEl = document.createElement('div');
      typingEl.className = 'ai-typing';
      typingEl.innerHTML = '<div class="ai-dot"></div><div class="ai-dot"></div><div class="ai-dot"></div><span style="margin-left:4px">Analisando documentos...</span>';
      messages.appendChild(typingEl);
      messages.scrollTop = messages.scrollHeight;

      const payload = {
        mensagem: text,
        conversaId: conversaId,
        senhaAcesso: senhaAcesso
      };

      // Tenta transmissão em tempo real via SSE
      let streamingSuccess = false;
      try {
        const streamRes = await fetch(`${apiBase}/api/publico/chat/${slug}/stream`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        if (streamRes.status === 401) {
          sessionStorage.removeItem('ai_widget_senha_' + slug);
          senhaAcesso = null;
          typingEl.remove();
          appendMessage('bot', 'Senha de acesso incorreta para este assistente.');
          isSending = false;
          send.disabled = false;
          return;
        }

        if (streamRes.ok && streamRes.body) {
          typingEl.remove();

          // Cria elemento da mensagem do bot para receber os tokens
          const botMsgEl = document.createElement('div');
          botMsgEl.className = 'ai-msg ai-msg-bot';
          botMsgEl.innerText = '';
          messages.appendChild(botMsgEl);

          let currentSources = [];
          const reader = streamRes.body.getReader();
          const decoder = new TextDecoder('utf-8');
          let buffer = '';

          while (true) {
            const { value, done } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop() || '';

            let currentEvent = 'message';
            for (const line of lines) {
              const trimmed = line.trim();
              if (!trimmed) continue;
              if (trimmed.startsWith('event:')) {
                currentEvent = trimmed.replace('event:', '').trim();
                continue;
              }
              if (trimmed.startsWith('data:')) {
                const raw = trimmed.replace('data:', '').trim();
                if (!raw) continue;
                try {
                  const data = JSON.parse(raw);
                  if (currentEvent === 'token' || data.type === 'token') {
                    botMsgEl.innerText += (data.content || '');
                    messages.scrollTop = messages.scrollHeight;
                  } else if (currentEvent === 'fontes' || data.type === 'fontes') {
                    currentSources = data.metadata?.fontes || [];
                  } else if (currentEvent === 'done' || data.type === 'done') {
                    if (data.metadata?.conversaId) {
                      conversaId = data.metadata.conversaId;
                      sessionStorage.setItem('ai_widget_conversa_' + slug, conversaId);
                    }
                  }
                } catch (e) {
                  // chunk parcial ignorado
                }
              }
            }
          }

          // Anexa fontes caso existam
          if (currentSources.length > 0) {
            renderSources(botMsgEl, currentSources);
          }

          streamingSuccess = true;
        }
      } catch (errStream) {
        console.warn('[AI Widget] Streaming indisponível, alternando para fallback REST:', errStream);
      }

      // Se o streaming não funcionou, fallback gracioso para POST padrão
      if (!streamingSuccess) {
        try {
          const response = await fetch(`${apiBase}/api/publico/chat/${slug}/mensagem`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
          });

          typingEl.remove();

          if (response.ok) {
            const data = await response.json();
            if (data.conversaId) {
              conversaId = data.conversaId;
              sessionStorage.setItem('ai_widget_conversa_' + slug, conversaId);
            }
            appendMessage('bot', data.resposta, data.fontes);
          } else if (response.status === 401) {
            sessionStorage.removeItem('ai_widget_senha_' + slug);
            senhaAcesso = null;
            appendMessage('bot', 'Senha de acesso incorreta para este assistente.');
          } else {
            appendMessage('bot', 'Desculpe, ocorreu um erro ao obter a resposta. Tente novamente mais tarde.');
          }
        } catch (err) {
          typingEl.remove();
          appendMessage('bot', 'Não foi possível conectar ao servidor corporativo.');
        }
      }

      isSending = false;
      send.disabled = false;
      input.focus();
    }

    send.onclick = handleSend;
    input.onkeydown = (e) => {
      if (e.key === 'Enter') handleSend();
    };

    function appendMessage(sender, text, fontes = []) {
      const msg = document.createElement('div');
      msg.className = `ai-msg ai-msg-${sender}`;
      msg.innerText = text;

      if (fontes && fontes.length > 0) {
        renderSources(msg, fontes);
      }

      messages.appendChild(msg);
      messages.scrollTop = messages.scrollHeight;
    }

    function renderSources(msgElement, fontes) {
      const f = document.createElement('div');
      f.className = 'ai-msg-sources';
      f.innerHTML = '<span>📚 Fontes:</span> ' + fontes.map(s => `<span class="ai-source-badge">${s}</span>`).join(' ');
      msgElement.appendChild(f);
      messages.scrollTop = messages.scrollHeight;
    }
  }
})();
