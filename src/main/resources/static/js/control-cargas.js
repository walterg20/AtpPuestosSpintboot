/* JS puntual para Control de Cargas. Sin frameworks, sin fetch/XHR. */
(function () {
  'use strict';

  function confirmarMarcado() {
    return confirm('Esta seguro que desea Marcar el formulario?');
  }

  function cerrarModal() {
    var modal = document.getElementById('modalDetalle');
    if (modal) {
      modal.style.display = 'none';
    }
  }

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      cerrarModal();
    }
  });

  // autofocus del barcode
  document.addEventListener('DOMContentLoaded', function () {
    var bc = document.getElementById('barCode');
    if (bc) {
      bc.focus();
    }
  });

  window.confirmarMarcado = confirmarMarcado;
  window.cerrarModal = cerrarModal;
})();
EOF