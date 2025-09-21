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
