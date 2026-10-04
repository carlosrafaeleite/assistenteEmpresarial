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
  let config = {
    corPrimaria: '#2563EB',
    corSecundaria: '#1E40AF',
    assistenteNome: 'Suporte IA',
    mensagemBoasVindas: 'Olá! Como posso ajudar você hoje?'
  };

  // Carrega configuração pública da empresa/assistente
  fetch(`${apiBase}/api/publico/chat/${slug}/config`)
    .then(res => res.ok ? res.json() : Promise.reject('Empresa não encontrada'))
    .then(data => {
      config = { ...config, ...data };
      initWidget();
    })
    .catch(err => {
      console.warn('[AI Widget] Falha ao obter configurações, usando padrões:', err);
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
        box-shadow: 0 4px 14px rgba(0,0,0,0.25);
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        border: none;
        color: #fff;
        transition: transform 0.2s ease;
      }
      #ai-chat-btn:hover {
        transform: scale(1.06);
      }
      #ai-chat-box {
        display: none;
        position: fixed;
        bottom: 96px;
        right: 24px;
        width: 380px;
        max-width: calc(100vw - 48px);
        height: 560px;
        max-height: calc(100vh - 120px);
        background: #ffffff;
        border-radius: 16px;
        box-shadow: 0 10px 25px rgba(0,0,0,0.18);
        overflow: hidden;
        flex-direction: column;
      }
      #ai-chat-header {
        background: ${config.corPrimaria};
        color: #ffffff;
        padding: 16px;
        display: flex;
        align-items: center;
        justify-content: space-between;
      }
      #ai-chat-header-title {
        font-weight: 600;
        font-size: 16px;
      }
      #ai-chat-close {
        background: transparent;
        border: none;
        color: #ffffff;
        font-size: 20px;
        cursor: pointer;
      }
      #ai-chat-messages {
        flex: 1;
        padding: 16px;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 12px;
        background: #f9fafb;
      }
      .ai-msg {
        max-width: 82%;
        padding: 10px 14px;
        border-radius: 14px;
        font-size: 14px;
        line-height: 1.45;
        word-break: break-word;
      }
      .ai-msg-bot {
        align-self: flex-start;
        background: #e5e7eb;
        color: #1f2937;
        border-bottom-left-radius: 2px;
      }
      .ai-msg-user {
        align-self: flex-end;
        background: ${config.corPrimaria};
        color: #ffffff;
        border-bottom-right-radius: 2px;
      }
      .ai-msg-sources {
        margin-top: 6px;
        font-size: 11px;
        opacity: 0.8;
        font-style: italic;
      }
      .ai-typing {
        font-size: 12px;
        color: #6b7280;
        font-style: italic;
      }
      #ai-chat-input-bar {
        display: flex;
        padding: 12px;
        border-top: 1px solid #e5e7eb;
        background: #ffffff;
        gap: 8px;
      }
      #ai-chat-input {
        flex: 1;
        border: 1px solid #d1d5db;
        border-radius: 20px;
        padding: 8px 14px;
        outline: none;
        font-size: 14px;
      }
      #ai-chat-input:focus {
        border-color: ${config.corPrimaria};
      }
      #ai-chat-send {
        background: ${config.corPrimaria};
        color: #fff;
        border: none;
        border-radius: 50%;
        width: 36px;
        height: 36px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    `;
    document.head.appendChild(style);

    const root = document.createElement('div');
    root.id = 'ai-chat-widget-root';
    root.innerHTML = `
      <div id="ai-chat-box">
        <div id="ai-chat-header">
          <div id="ai-chat-header-title">${config.assistenteNome}</div>
          <button id="ai-chat-close">&times;</button>
        </div>
        <div id="ai-chat-messages">
          <div class="ai-msg ai-msg-bot">${config.mensagemBoasVindas}</div>
        </div>
        <div id="ai-chat-input-bar">
          <input type="text" id="ai-chat-input" placeholder="Digite sua mensagem..." autocomplete="off"/>
          <button id="ai-chat-send">&#10148;</button>
        </div>
      </div>
      <button id="ai-chat-btn" aria-label="Abrir Chat">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
      if (!text) return;

      input.value = '';

      // Adiciona mensagem do usuário
      appendMessage('user', text);

      // Adiciona indicador de digitação
      const typingEl = document.createElement('div');
      typingEl.className = 'ai-typing';
      typingEl.innerText = 'Consultando documentos e digitando...';
      messages.appendChild(typingEl);
      messages.scrollTop = messages.scrollHeight;

      try {
        const response = await fetch(`${apiBase}/api/publico/chat/${slug}/mensagem`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            mensagem: text,
            conversaId: conversaId
          })
        });

        typingEl.remove();

        if (response.ok) {
          const data = await response.json();
          if (data.conversaId) {
            conversaId = data.conversaId;
            sessionStorage.setItem('ai_widget_conversa_' + slug, conversaId);
          }
          appendMessage('bot', data.resposta, data.fontes);
        } else {
          appendMessage('bot', 'Desculpe, ocorreu um erro ao obter a resposta. Tente novamente mais tarde.');
        }
      } catch (err) {
        typingEl.remove();
        appendMessage('bot', 'Não foi possível conectar ao servidor.');
      }
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
        const f = document.createElement('div');
        f.className = 'ai-msg-sources';
        f.innerText = 'Fontes consultadas: ' + fontes.join(', ');
        msg.appendChild(f);
      }

      messages.appendChild(msg);
      messages.scrollTop = messages.scrollHeight;
    }
  }
})();
