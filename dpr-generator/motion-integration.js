/**
 * Motion Animations Integration for UdyamCare DPR Generator
 *
 * This module adds smooth animations using the Motion library
 * to enhance the user experience of the DPR form and report generation.
 *
 * Usage: Import this module in index.html after Motion is loaded
 */

// Motion Animation Utilities for DPR Generator
class DPRMotionAnimations {
  constructor() {
    this.isEnabled = true;
  }

  /**
   * Initialize animations for the DPR form and report sections
   */
  init() {
    if (typeof motion === 'undefined') {
      console.warn('Motion library not available. Animations disabled.');
      return;
    }

    this.setupFormAnimations();
    this.setupReportAnimations();
    this.setupTabAnimations();
    this.setupButtonAnimations();
  }

  /**
   * Animate form sections as they come into view
   */
  setupFormAnimations() {
    const formSections = document.querySelectorAll('details');

    formSections.forEach((section, index) => {
      section.style.opacity = '0';
      section.style.transform = 'translateY(20px)';

      // Stagger the animations
      setTimeout(() => {
        if (this.isEnabled && motion && motion.animate) {
          motion.animate(section, {
            opacity: [0, 1],
            transform: ['translateY(20px)', 'translateY(0)']
          }, {
            duration: 0.6,
            delay: index * 0.1,
            easing: 'ease-out'
          });
        }
      }, 100);
    });
  }

  /**
   * Animate report generation elements
   */
  setupReportAnimations() {
    // Listen for report generation completion
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.type === 'childList' && mutation.addedNodes.length) {
          const reportElement = document.getElementById('tab-report');
          if (reportElement && reportElement.style.display !== 'none') {
            this.animateReportContent();
          }
        }
      });
    });

    const config = { childList: true, subtree: true };
    observer.observe(document.body, config);
  }

  /**
   * Animate individual report content items
   */
  animateReportContent() {
    const sections = document.querySelectorAll('[class*="section"]');

    sections.forEach((section, index) => {
      if (!section._animated) {
        section._animated = true;
        section.style.opacity = '0';
        section.style.transform = 'translateX(-20px)';

        setTimeout(() => {
          if (this.isEnabled && motion && motion.animate) {
            motion.animate(section, {
              opacity: [0, 1],
              transform: ['translateX(-20px)', 'translateX(0)']
            }, {
              duration: 0.5,
              delay: index * 0.05,
              easing: 'ease-out'
            });
          }
        }, 50);
      }
    });
  }

  /**
   * Animate tab switching
   */
  setupTabAnimations() {
    const tabs = document.querySelectorAll('.tab');

    tabs.forEach(tab => {
      tab.addEventListener('click', (e) => {
        const targetTab = e.target.getAttribute('data-tab');
        const targetPanel = document.getElementById(`tab-${targetTab}`);

        if (targetPanel && this.isEnabled && motion && motion.animate) {
          motion.animate(targetPanel, {
            opacity: [0, 1],
            transform: ['translateY(10px)', 'translateY(0)']
          }, {
            duration: 0.4,
            easing: 'ease-out'
          });
        }
      });
    });
  }

  /**
   * Add hover animations to buttons
   */
  setupButtonAnimations() {
    const buttons = document.querySelectorAll('button');

    buttons.forEach(button => {
      button.addEventListener('mouseenter', (e) => {
        if (this.isEnabled && motion && motion.animate) {
          motion.animate(e.target, {
            transform: ['scale(1)', 'scale(1.05)']
          }, {
            duration: 0.2,
            easing: 'ease-out'
          });
        }
      });

      button.addEventListener('mouseleave', (e) => {
        if (this.isEnabled && motion && motion.animate) {
          motion.animate(e.target, {
            transform: ['scale(1.05)', 'scale(1)']
          }, {
            duration: 0.2,
            easing: 'ease-out'
          });
        }
      });
    });
  }

  /**
   * Animate success message after report generation
   */
  animateSuccess(element) {
    if (!this.isEnabled || !motion || !motion.animate) return;

    motion.animate(element, {
      opacity: [0, 1],
      transform: ['scale(0.8)', 'scale(1)']
    }, {
      duration: 0.5,
      easing: 'ease-out'
    });
  }

  /**
   * Animate loading state
   */
  animateLoading(element) {
    if (!this.isEnabled || !motion || !motion.animate) return;

    motion.animate(element, {
      transform: ['rotate(0deg)', 'rotate(360deg)']
    }, {
      duration: 1,
      repeat: Infinity,
      easing: 'linear'
    });
  }

  /**
   * Toggle animations on/off
   */
  toggleAnimations(enabled) {
    this.isEnabled = enabled;
  }
}

// Export for use in modules
if (typeof module !== 'undefined' && module.exports) {
  module.exports = DPRMotionAnimations;
}

// Auto-initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  window.dprMotions = new DPRMotionAnimations();
  window.dprMotions.init();
});
