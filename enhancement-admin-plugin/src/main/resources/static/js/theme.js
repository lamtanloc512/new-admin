(function ($) {
  setTimeout(function () {
    $(function () {
      $(".progressive-image").progressiveImage();
    });
    $(window).on("resize", function () {
      $(".progressive-image").progressiveImage();
    });
  }, 500);
})(jQuery);

(function ($) {
  $(window).on("reload", function () {
    setTimeout(function () {
      $(".progressive-image").progressiveImage();
    }, 500);
  });
})(jQuery);
