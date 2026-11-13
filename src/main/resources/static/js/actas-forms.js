/*
 * Carga Manual de Actas — JS puntual (Stitch SCREEN_21).
 * Vanilla, sin framework, sin bundler, sin recursos externos.
 */
(function () {
  'use strict';

  var ID_PLANTILLA = 'acta-producto-plantilla';
  var ID_CUERPO = 'acta-productos-body';
  var MARCADOR_INDICE = '__INDEX__';
  var MARCADOR_FILA = '__FILA__';
  var RE_NOMBRE = /productos\[\d+\]/;
  var MENSAJE_CONFIRMACION = '¿Confirma que desea enviar el Formulario de Acta?';

  function todos(selector, raiz) {
    return Array.prototype.slice.call((raiz || document).querySelectorAll(selector));
  }

  function idDe(nombre) {
    return nombre.replace(/productos\[(\d+)\]\.(\w+)/, 'productos_$1_$2');
  }

  function controles(fila) {
    return todos('input, select, textarea', fila);
  }

  function renumerar(cuerpo) {
    todos('tr[data-acta-producto]', cuerpo).forEach(function (fila, indice) {
      controles(fila).forEach(function (control) {
        var nombre = control.getAttribute('name');
        if (!nombre) {
          return;
        }
        var idAnterior = control.getAttribute('id');
        var nuevo = nombre.replace(RE_NOMBRE, 'productos[' + indice + ']');
        control.setAttribute('name', nuevo);
        var idNuevo = idDe(nuevo);
        control.setAttribute('id', idNuevo);
        if (idAnterior) {
          var etiqueta = fila.querySelector('label[for="' + idAnterior + '"]');
          if (etiqueta) {
            etiqueta.setAttribute('for', idNuevo);
            var rotulo = etiqueta.getAttribute('data-rotulo');
            if (rotulo) {
              etiqueta.textContent = rotulo + ' fila ' + (indice + 1);
            }
          }
        }
      });
      var boton = fila.querySelector('.js-quitar-producto');
      if (boton) {
        boton.setAttribute('data-indice', String(indice));
        boton.setAttribute('aria-label', 'Eliminar fila ' + (indice + 1));
      }
    });
  }

  function agregarFila(cuerpo) {
    var plantilla = document.getElementById(ID_PLANTILLA);
    if (!plantilla || !plantilla.content) {
      return;
    }
    var fragmento = plantilla.content.cloneNode(true);
    var fila = fragmento.querySelector('tr');
    if (!fila) {
      return;
    }
    var indice = todos('tr[data-acta-producto]', cuerpo).length;
    controles(fila).forEach(function (control) {
      var nombre = control.getAttribute('name');
      if (nombre) {
        control.setAttribute('name', nombre.replace(MARCADOR_INDICE, String(indice)));
      }
      var id = control.getAttribute('id');
      if (id) {
        control.setAttribute('id', id.replace(MARCADOR_INDICE, String(indice)));
      }
      control.value = '';
    });
    todos('label[data-rotulo]', fila).forEach(function (etiqueta) {
      var id = etiqueta.getAttribute('for');
      if (id) {
        etiqueta.setAttribute('for', id.replace(MARCADOR_INDICE, String(indice)));
      }
      etiqueta.textContent = etiqueta.getAttribute('data-rotulo') + ' fila ' + (indice + 1);
    });
    var boton = fila.querySelector('.js-quitar-producto');
    if (boton) {
      boton.setAttribute('data-indice', String(indice));
      boton.setAttribute('aria-label', 'Eliminar fila ' + (indice + 1));
    }
    cuerpo.appendChild(fila);
    renumerar(cuerpo);
    updateTotals();
  }

  function quitarFila(fila, cuerpo) {
    if (fila && fila.parentNode) {
      fila.parentNode.removeChild(fila);
    }
    renumerar(cuerpo);
    updateTotals();
  }

  function updateTotals() {
    var total = 0;
    todos('[data-cantidad="true"], [data-cantidad]', document).forEach(function (input) {
      var valor = parseFloat(input.value);
      if (!isNaN(valor)) {
        total += valor;
      }
    });
    var salida = document.getElementById('acta-total-toneladas');
    if (salida) {
      salida.textContent = total.toFixed(4);
    }
  }

  function sincronizarFila(fila) {
    var select = fila.querySelector('[data-producto]');
    if (!select) {
      return;
    }
    var opcion = select.selectedIndex >= 0 ? select.options[select.selectedIndex] : null;
    var descripcion = fila.querySelector('[data-descripcion]');
    if (descripcion) {
      descripcion.textContent = opcion ? (opcion.getAttribute('data-descripcion') || '') : '';
    }
    var cantidad = fila.querySelector('[data-cantidad]');
    if (!cantidad) {
      return;
    }
    var tipoCalculo = opcion ? opcion.getAttribute('data-tipcal') : null;
    if (tipoCalculo === '3') {
      cantidad.value = '1';
      cantidad.setAttribute('readonly', 'readonly');
    } else {
      cantidad.removeAttribute('readonly');
    }
    updateTotals();
  }

  function activarGrilla() {
    var cuerpo = document.getElementById(ID_CUERPO);
    if (!cuerpo) {
      return;
    }
    var botonAgregar = document.getElementById('acta-agregar-producto');
    if (botonAgregar) {
      botonAgregar.addEventListener('click', function () {
        agregarFila(cuerpo);
      });
    }
    cuerpo.addEventListener('click', function (evento) {
      var boton = evento.target.closest('.js-quitar-producto');
      if (!boton) {
        return;
      }
      quitarFila(boton.closest('tr'), cuerpo);
    });
    cuerpo.addEventListener('change', function (evento) {
      var select = evento.target.closest('[data-producto]');
      if (select) {
        sincronizarFila(select.closest('tr'));
      }
      if (evento.target.matches('[data-cantidad]')) {
        updateTotals();
      }
    });
    cuerpo.addEventListener('input', function (evento) {
      if (evento.target.matches('[data-cantidad]')) {
        updateTotals();
      }
    });
    todos('tr[data-acta-producto]', cuerpo).forEach(function (fila) {
      var select = fila.querySelector('[data-producto]');
      if (select && select.selectedIndex > 0) {
        sincronizarFila(fila);
      }
    });
    updateTotals();
  }

  function activarBusquedaCuit() {
    todos('[data-cuit-search], .actas-cuit-buscar').forEach(function (boton) {
      boton.addEventListener('click', function () {
        window.alert('Búsqueda de CUIT no implementada');
      });
    });
  }

  function activarConfirmacion() {
    var formulario = document.querySelector('form.actas-forms__form');
    if (!formulario) {
      return;
    }
    formulario.addEventListener('submit', function (evento) {
      if (!window.confirm(MENSAJE_CONFIRMACION)) {
        evento.preventDefault();
      }
    });
    var boton = document.getElementById('actas-confirmar');
    if (boton) {
      boton.addEventListener('click', function () {
        if (typeof formulario.requestSubmit === 'function') {
          formulario.requestSubmit();
        } else {
          formulario.submit();
        }
      });
    }
    var imprimir = document.getElementById('actas-imprimir');
    if (imprimir) {
      imprimir.addEventListener('click', function () {
        window.print();
      });
    }
  }

  function activar() {
    activarGrilla();
    activarBusquedaCuit();
    activarConfirmacion();
    updateTotals();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', activar);
  } else {
    activar();
  }
})();