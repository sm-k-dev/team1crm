// UI-only navigation enhancement; existing fetch/auth code is unchanged.
(function () {
  const sidebar = document.getElementById('saasSidebar');
  const toggle = document.querySelector('.saas-menu-toggle');
  const overlay = document.querySelector('.saas-overlay');
  if (!sidebar || !toggle || !overlay) return;
  const close = () => { sidebar.classList.remove('open'); overlay.classList.remove('open'); toggle.setAttribute('aria-expanded','false'); };
  toggle.addEventListener('click', () => { const opened = sidebar.classList.toggle('open'); overlay.classList.toggle('open',opened); toggle.setAttribute('aria-expanded',String(opened)); });
  overlay.addEventListener('click',close);
  document.addEventListener('keydown', e => { if(e.key === 'Escape') close(); });
  const path = window.location.pathname;
  document.querySelectorAll('.saas-side-link[data-route]').forEach(a => { const route=a.getAttribute('data-route'); if(route==='/' ? path==='/' : path===route || path.startsWith(route+'/')) {a.classList.add('active');a.setAttribute('aria-current','page');} });
})();
