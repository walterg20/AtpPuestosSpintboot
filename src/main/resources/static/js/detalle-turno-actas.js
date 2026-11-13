/*
 * JS puntual de la solapa LISTADO ACTAS:
 *   - filtrado en vivo de la grilla por texto
 *   - acciones Ver/Imprimir sin funcionalidad (sin lógica de negocio)
 */
(function () {
  'use strict';

  function normalizar(texto) {
    return (texto || '').toString().toLowerCase().trim();
  }

  function filtrar() {
    var input = document.getElementById('dta-busqueda');
    var tabla = document.getElementById('dta-tabla');
    if (!input || !tabla) {
      return;
    }
    var termino = normalizar(input.value);
    var filas = tabla.querySelectorAll('tbody tr');
    Array.prototype.forEach.call(filas, function (fila) {
      var texto = normalizar(fila.textContent);
      fila.style.display = (termino === '' || texto.indexOf(termino) !== -1) ? '' : 'none';
    });
  }

  function activarFiltro() {
    var input = document.getElementById('dta-busqueda');
    if (input) {
      input.addEventListener('input', filtrar);
      input.addEventListener('keyup', filtrar);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', activarFiltro);
  } else {
    activarFiltro();
  }
})();