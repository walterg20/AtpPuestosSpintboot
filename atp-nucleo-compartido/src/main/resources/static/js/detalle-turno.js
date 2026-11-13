/*
 * JS puntual de la pantalla Detalle de Turno:
 *   - apertura/cierre del modal de cierre de turno
 *   - envio del formulario al confirmar
 *   - cambio de pestañas (resaltado)
 */
(function () {
  'use strict';

  function abrirModal(id) {
    var modal = document.getElementById(id);
    if (modal) {
      modal.hidden = false;
    }
  }

  function cerrarModales() {
    var modales = document.querySelectorAll('.dt-modal');
    Array.prototype.forEach.call(modales, function (m) {
      m.hidden = true;
    });
  }

  function activarModal() {
    var disparadores = document.querySelectorAll('[data-abrir-modal]');
    Array.prototype.forEach.call(disparadores, function (btn) {
      btn.addEventListener('click', function () {
        abrirModal(btn.getAttribute('data-abrir-modal'));
      });
    });

    var cierres = document.querySelectorAll('[data-cerrar-modal]');
    Array.prototype.forEach.call(cierres, function (el) {
      el.addEventListener('click', cerrarModales);
    });

    var confirmar = document.querySelector('[data-confirmar-cierre]');
    if (confirmar) {
      confirmar.addEventListener('click', function () {
        var form = document.getElementById('form-cerrar-turno');
        if (form) {
          form.submit();
        }
        cerrarModales();
      });
    }
  }

  function activarTabs() {
    var tabs = document.querySelectorAll('.dt-tab');
    Array.prototype.forEach.call(tabs, function (tab) {
      tab.addEventListener('click', function () {
        tabs.forEach(function (t) { t.classList.remove('dt-tab--active'); });
        tab.classList.add('dt-tab--active');
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () {
      activarModal();
      activarTabs();
    });
  } else {
    activarModal();
    activarTabs();
  }
})();