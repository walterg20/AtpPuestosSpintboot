/* modal-detalle-guia.js - vanilla, sin imports ni fetch */
(function () {
  'use strict';

  function getModal() {
    return document.getElementById('modalDetalle');
  }

  function abrirModalDetalleGuia() {
    var m = getModal();
    if (!m) return;
    m.classList.remove('hidden');
    document.body.style.overflow = 'hidden';
  }

  function cerrarModalDetalleGuia() {
    var m = getModal();
    if (!m) return;
    m.classList.add('hidden');
    document.body.style.overflow = '';
  }

  function imprimirModalDetalleGuia() {
    window.print();
  }

  function descargarModalDetalleGuia() {
    window.alert('Descarga disponible proximamente.');
  }

  function init() {
    var m = getModal();
    if (!m) return;

    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') {
        if (m && !m.classList.contains('hidden')) {
          cerrarModalDetalleGuia();
        }
      }
    });

    document.addEventListener('click', function (e) {
      if (e.target === m) {
        cerrarModalDetalleGuia();
      }
    });

    window.abrirModalDetalleGuia = abrirModalDetalleGuia;
    window.cerrarModalDetalleGuia = cerrarModalDetalleGuia;
    window.imprimirModalDetalleGuia = imprimirModalDetalleGuia;
    window.descargarModalDetalleGuia = descargarModalDetalleGuia;

    // No auto-open: el modal se renderiza visible por servidor (mostrarModal=true).
    // Se limpia la URL para evitar re-apertura al recargar.
    if (window.history && window.history.replaceState) {
      window.history.replaceState(null, document.title, window.location.pathname);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
