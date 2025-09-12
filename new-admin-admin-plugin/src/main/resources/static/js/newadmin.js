/*!
 * Copyright 2022 youngmonkeys.org
 *
 * Licensed under the ezyplatform, Version 1.0.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://youngmonkeys.org/licenses/ezyplatform-1.0.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// ============== messages ==================
ezyadmin.getI18nMessage = function (key, toLowerCase) {
  var answer = ezyadmin.messages[key] || key;
  if (toLowerCase) {
    answer = answer.toLowerCase();
  } else {
    answer = answer.substring(0, 1).toUpperCase() + answer.substring(1);
  }
  return answer;
};

// ============== datetime ==================
ezyadmin.currentTimeMillis = function () {
  return new Date().getTime();
};

ezyadmin.formatTimeStamp = function (timestamp, format) {
  if (!timestamp) {
    return "";
  }
  var actualFormat =
    format || ezyadmin.defaultDateTimeFormat || "YYYY-MM-DD HH:mm:ss";
  return moment(timestamp).format(actualFormat);
};

ezyadmin.formatTimeStampDate = function (timestamp) {
  return ezyadmin.formatTimeStamp(
    parseInt(timestamp, 10),
    ezyadmin.defaultDateFormat || "YYYY-MM-DD"
  );
};

ezyadmin.formatTimeStampTime = function (timestamp) {
  return ezyadmin.formatTimeStamp(
    parseInt(timestamp, 10),
    ezyadmin.defaultTimeFormat || "HH:mm:ss"
  );
};

ezyadmin.formatTimeStampMinute = function (timestamp) {
  return ezyadmin.formatTimeStamp(
    parseInt(timestamp, 10),
    ezyadmin.defaultDateMinuteFormat || "YYYY-MM-DD HH:mm"
  );
};

ezyadmin.durationToString = function (duration, separator) {
  var remain = duration;
  var years = (remain / (365 * 24 * 60 * 60)) | 0;
  remain = remain % (365 * 24 * 60 * 60);
  var months = (remain / (30 * 24 * 60 * 60)) | 0;
  remain = remain % (30 * 24 * 60 * 60);
  var days = (remain / (24 * 60 * 60)) | 0;
  remain = remain % (24 * 60 * 60);
  var hours = (remain / (60 * 60)) | 0;
  remain = remain % (60 * 60);
  var minutes = (remain / 60) | 0;
  var seconds = remain % 60;

  if (years || months || days || hours || minutes) {
    if (separator && seconds < 10) {
      seconds = "0" + seconds;
    }
  }
  var answer = seconds + (separator ? "" : "s");
  if (years || months || days || hours) {
    if (separator && minutes < 10) {
      minutes = "0" + minutes;
    }
  }
  if (years || months || days || hours || minutes) {
    answer = minutes + (separator || "m:") + answer;
  }
  if (years || months || days) {
    if (separator && hours < 10) {
      hours = "0" + hours;
    }
  }
  if (years || months || days || hours) {
    answer = hours + (separator || "h:") + answer;
  }
  if (years || months) {
    if (separator && days < 10) {
      days = "0" + days;
    }
  }
  if (years || months || days) {
    answer = days + (separator || "d ") + answer;
  }
  if (years) {
    if (separator && months < 10) {
      months = "0" + months;
    }
  }
  if (years || months) {
    answer = months + (separator || "m-") + answer;
  }
  if (years) {
    answer = years + (separator || "y-") + answer;
  }
  return answer;
};

ezyadmin.durationToSimpleString = function (duration) {
  var remain = duration;
  var years = (remain / (365 * 24 * 60 * 60)) | 0;
  remain = remain % (365 * 24 * 60 * 60);
  var months = (remain / (30 * 24 * 60 * 60)) | 0;
  remain = remain % (30 * 24 * 60 * 60);
  var weeks = (remain / (7 * 24 * 60 * 60)) | 0;
  remain = remain % (7 * 24 * 60 * 60);
  var days = (remain / (24 * 60 * 60)) | 0;
  remain = remain % (24 * 60 * 60);
  var hours = (remain / (60 * 60)) | 0;
  remain = remain % (60 * 60);
  var minutes = (remain / 60) | 0;
  var seconds = remain % 60;

  var answer = "0s";
  if (years || months || weeks || days || hours || minutes || seconds) {
    answer = seconds + "s";
  }
  if (years || months || weeks || days || hours || minutes) {
    answer = minutes + "m" + answer;
  }
  if (years || months || weeks || days || hours) {
    answer = hours + "h" + answer;
  }
  if (years || months || weeks || days) {
    answer = days + "d" + answer;
  }
  if (years || months || weeks) {
    answer = weeks + "w" + answer;
  }
  if (years || months) {
    answer = months + "M" + answer;
  }
  if (years) {
    answer = years + "y" + answer;
  }
  return answer;
};

ezyadmin.simpleDurationStringToMillis = function (str) {
  if (!str) {
    return 0;
  }
  var answer = 0;
  var millisBySymbol = {
    ms: 1,
    s: 1000,
    m: 60 * 1000,
    h: 60 * 60 * 1000,
    d: 24 * 60 * 60 * 1000,
    w: 7 * 24 * 60 * 60 * 1000,
    M: 30 * 24 * 60 * 60 * 1000,
    y: 365 * 24 * 60 * 60 * 1000,
  };
  var number = "";
  var lastSymbol = null;
  for (var i = 0; i < str.length; ++i) {
    if (str[i] >= "0" && str[i] <= "9") {
      number += str[i];
      lastSymbol = null;
    } else {
      lastSymbol = str[i];
      if (i + 1 < str.length) {
        if (str[i + 1] < "0" || str[i + 1] > "9") {
          lastSymbol += str[i + 1];
          ++i;
        }
      }
      var millis = millisBySymbol[lastSymbol];
      if (millis && number) {
        answer += parseInt(number, 10) * millis;
        number = "";
      } else {
        answer = -1;
        break;
      }
    }
  }
  if (!lastSymbol) {
    answer = -1;
  }
  return answer;
};

ezyadmin.getTimeRangeValue = function (milestoneTime) {
  return new Date().getTime() - milestoneTime;
};

ezyadmin.getTimeRangeTextValue = function (milestoneTime, includeSuffix) {
  var timeRangeValue = ezyadmin.getTimeRangeValue(milestoneTime);
  var ago = timeRangeValue > 0;
  var timeRange = Math.abs(timeRangeValue) / 1000;
  var remain = timeRange;
  var years = (remain / (365 * 24 * 60 * 60)) | 0;
  remain = remain % (365 * 24 * 60 * 60);
  var months = (remain / (30 * 24 * 60 * 60)) | 0;
  remain = remain % (30 * 24 * 60 * 60);
  var days = (remain / (24 * 60 * 60)) | 0;
  remain = remain % (24 * 60 * 60);
  var hours = (remain / (60 * 60)) | 0;
  remain = remain % (60 * 60);
  var minutes = (remain / 60) | 0;
  var seconds = remain % 60 | 0;

  var answer = "1 " + ezyadmin.getI18nMessage("second", true);
  if (years) {
    if (years == 1) {
      answer = years + " " + ezyadmin.getI18nMessage("year", true);
    } else {
      answer = years + " " + ezyadmin.getI18nMessage("years", true);
    }
  } else if (months) {
    if (months == 1) {
      answer = months + " " + ezyadmin.getI18nMessage("month", true);
    } else {
      answer = months + " " + ezyadmin.getI18nMessage("months", true);
    }
  } else if (days) {
    if (days == 1) {
      answer = days + " " + ezyadmin.getI18nMessage("day", true);
    } else {
      answer = days + " " + ezyadmin.getI18nMessage("days", true);
    }
  } else if (hours) {
    if (hours == 1) {
      answer = hours + " " + ezyadmin.getI18nMessage("hour", true);
    } else {
      answer = hours + " " + ezyadmin.getI18nMessage("hours", true);
    }
  } else if (minutes) {
    if (minutes == 1) {
      answer = minutes + " " + ezyadmin.getI18nMessage("minute", true);
    } else {
      answer = minutes + " " + ezyadmin.getI18nMessage("minutes", true);
    }
  } else if (seconds) {
    if (seconds == 1) {
      answer = seconds + " " + ezyadmin.getI18nMessage("second", true);
    } else {
      answer = seconds + " " + ezyadmin.getI18nMessage("seconds", true);
    }
  }
  if (includeSuffix == false) {
    return answer;
  }
  return (
    answer +
    " " +
    (ago
      ? ezyadmin.getI18nMessage("ago", true)
      : ezyadmin.getI18nMessage("more", true))
  );
};

// ============== util ======================
ezyadmin.formatNumberWithCommas = function (input) {
  input = ("" + input).replace(/,/g, "");
  var parts = input.split(".");
  var integerPart = parts[0];
  var decimalPart = parts.length > 1 ? "." + parts[1] : "";
  integerPart = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return integerPart + decimalPart;
};

ezyadmin.listToBooleanMap = function (list) {
  var map = {};
  list.forEach((item) => {
    map[item] = true;
  });
  return map;
};

ezyadmin.removeItemFromList = function (list, itemToRemove) {
  var index = list.indexOf(itemToRemove);
  if (index > -1) {
    list.splice(index, 1);
  }
};

ezyadmin.moduleContainerFolders = {
  "admin-plugin": "plugins",
  "socket-plugin": "plugins",
  "socket-app": "apps",
  "web-plugin": "plugins",
  theme: "themes",
};

ezyadmin.moduleTypeShortNames = {
  "admin-plugin": "Plugin",
  "socket-plugin": "Plugin",
  "socket-app": "App",
  "web-plugin": "Plugin",
  theme: "Theme",
};

ezyadmin.moduleTypeNames = {
  "admin-plugin": "Admin Plugin",
  "socket-plugin": "Socket Plugin",
  "socket-app": "Socket Application",
  "web-plugin": "Web Plugin",
  theme: "Theme",
};

ezyadmin.getModuleTypeShortName = function (moduleType) {
  return (
    ezyadmin.moduleTypeShortNames[moduleType] ||
    ezyadmin.toCapitalizeString(moduleType)
  );
};

ezyadmin.getModuleTypeName = function (moduleType) {
  return (
    ezyadmin.moduleTypeNames[moduleType] ||
    ezyadmin.toCapitalizeString(moduleType)
  );
};

ezyadmin.getModuleTypeNames = function (moduleTypes) {
  var moduleTypeNames = [];
  moduleTypes.forEach((moduleType) => {
    moduleTypeNames.push(ezyadmin.getModuleTypeName(moduleType));
  });
  return moduleTypeNames.join(", ");
};

ezyadmin.errorMessageByCode = {
  required: ezyadmin.getI18nMessage("required"),
  invalid: ezyadmin.getI18nMessage("invalid"),
  notFound: ezyadmin.getI18nMessage("not_found"),
  tooShort: ezyadmin.getI18nMessage("too_short"),
  tooLong: ezyadmin.getI18nMessage("too_long"),
  overLength: ezyadmin.getI18nMessage("over_length"),
  duplicated: ezyadmin.getI18nMessage("duplicated"),
  incorrect: ezyadmin.getI18nMessage("incorrect"),
  mismatch: ezyadmin.getI18nMessage("mismatch"),
  tooMany: ezyadmin.getI18nMessage("too_many"),
  rejected: ezyadmin.getI18nMessage("rejected"),
};

ezyadmin.getErrorMessageByCode = function (code) {
  var errorMessage = ezyadmin.errorMessageByCode[code];
  return errorMessage || ezyadmin.getI18nMessage("invalid");
};

ezyadmin.textColorByStatus = {
  ACTIVATED: "text-success",
  ARCHIVED: "text-info",
  APPROVED: "text-success",
  APPROVING: "text-primary",
  BLOCKING: "text-primary",
  BLOCKED: "text-danger",
  BURNED: "text-secondary",
  CLOSED: "text-danger",
  DELETED: "text-danger",
  INACTIVATED: "text-secondary",
  MINTED: "text-info",
  OPENED: "text-success",
  PUBLISHED: "text-success",
  REGISTER_OPENED: "text-success",
  REGISTER_CLOSED: "text-danger",
  REJECTED: "text-danger",
  REVIEWING: "text-primary",
  RELEASABLE: "text-info",
  RELEASED: "text-success",
};

ezyadmin.getTextColorByStatus = function (status) {
  var textColor = ezyadmin.textColorByStatus[status];
  return textColor || "";
};

ezyadmin.toDisplayString = function (str) {
  if (!str) {
    return "";
  }
  var answer = str
    .replaceAll(".", " ")
    .replaceAll("-", " ")
    .replaceAll("_", " ")
    .replaceAll("  ", " ");
  if (answer.length <= 1) {
    return answer;
  }
  return answer[0].toUpperCase() + answer.substring(1);
};

ezyadmin.toCapitalizeString = function (str) {
  var displayString = ezyadmin.toDisplayString(str);
  var arr = str.split(" ");
  for (var i = 0; i < arr.length; i++) {
    arr[i] = arr[i].charAt(0).toUpperCase() + arr[i].slice(1);
  }
  return arr.join(" ");
};

ezyadmin.toHeadingString = function (str) {
  return ezyadmin.toCapitalizeString(ezyadmin.toDisplayString(str));
};

ezyadmin.truncateString = function (str, maxLength, suffix) {
  if (!str) {
    return "";
  }
  if (str.length <= maxLength) {
    return str;
  }
  var answer = str.substring(0, maxLength);
  if (suffix) {
    answer += " " + suffix;
  }
  return answer;
};

ezyadmin.getHtmlBodyContent = function (html) {
  if (!html) {
    return "";
  }
  var bodyTagOpen = "<body>";
  var bodyTagClose = "</body>";
  var openIndex = html.indexOf(bodyTagOpen);
  var closeIndex = html.indexOf(bodyTagClose);
  if (openIndex >= 0 && closeIndex > 0) {
    return html.substring(openIndex + bodyTagOpen.length(), closeIndex);
  }
  return html;
};

ezyadmin.removeHtmlTagAndSub = function (html, maxLength, ellipsis) {
  if (!html) {
    return "";
  }
  var answer = "";
  var endWord = false;
  var isTagContent = false;
  for (var i = 0; i < html.length; ++i) {
    if (html[i] == "<") {
      isTagContent = true;
    }
    if (isTagContent) {
      if (html[i] == ">") {
        isTagContent = false;
      }
      if (answer.length > 1 && answer[answer.length - 1] != " ") {
        answer += " ";
      }
      continue;
    }
    if (html[i] == "\t" || html[i] == "\n" || html[i] == " ") {
      if (answer.length > 1 && answer[answer.length - 1] != " ") {
        answer += " ";
      }
      endWord = true;
    } else {
      answer += html[i];
      endWord = false;
    }
    if (answer.length >= maxLength && endWord) {
      break;
    }
  }
  return answer + (ellipsis != false ? " ..." : "");
};

ezyadmin.normalCharacters =
  "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
ezyadmin.randomString = function (length, characters) {
  var result = "";
  var characters = characters || ezyadmin.normalCharacters;
  var charactersLength = characters.length;
  for (var i = 0; i < length; i++) {
    result += characters.charAt(Math.floor(Math.random() * charactersLength));
  }
  return result;
};

ezyadmin.passwordCharacters =
  "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789~!@#$%^&*()_+";
ezyadmin.randomPassword = function (length) {
  return ezyadmin.randomString(length || 16, ezyadmin.passwordCharacters);
};

ezyadmin.appendLangParameterToUrl = function (url) {
  if (ezyadmin.lang) {
    return url.includes("?")
      ? url + "&lang=" + ezyadmin.lang
      : url + "?lang=" + ezyadmin.lang;
  }
  return url;
};

ezyadmin.wrapTextIfNeed = function (text, tag, need) {
  return need ? "<" + tag + ">" + text + "</" + tag + ">" : text;
};

ezyadmin.getCurrentUrlParameter = function (paramName) {
  return new URL(document.location).searchParams.get(paramName);
};

ezyadmin.joinToString = function (array, separator, itemMapper) {
  var str = "";
  for (var i = 0; i < array.length; ++i) {
    str += itemMapper ? itemMapper(array[i]) : array[i];
    if (i < array.length - 1) {
      str += separator;
    }
  }
  return str;
};

ezyadmin.mergeObjects = function (objects) {
  var answer = {};
  objects.forEach((obj) => {
    Object.entries(obj).forEach(([key, value]) => {
      answer[key] = value;
    });
  });
  return answer;
};

ezyadmin.mergeObjectsToFirst = function (objects) {
  var answer = objects[0];
  for (var i = 1; i < objects.length; ++i) {
    Object.entries(objects[i]).forEach(([key, value]) => {
      answer[key] = value;
    });
  }
};

ezyadmin.deepMergeObjects = function (target, ...sources) {
  if (typeof target !== "object" || target === null) {
    return target;
  }
  var stack = [];
  for (var source of sources) {
    if (typeof source !== "object" || source === null) {
      continue;
    }
    stack.push({ target, source });
  }
  while (stack.length) {
    var { target: currentTarget, source: currentSource } = stack.pop();
    for (const key in currentSource) {
      if (!Object.prototype.hasOwnProperty.call(currentSource, key)) {
        continue;
      }
      const srcVal = currentSource[key];
      const tgtVal = currentTarget[key];
      if (
        typeof tgtVal === "object" &&
        typeof srcVal === "object" &&
        tgtVal !== null &&
        srcVal !== null &&
        !Array.isArray(tgtVal) &&
        !Array.isArray(srcVal)
      ) {
        stack.push({ target: tgtVal, source: srcVal });
      } else {
        currentTarget[key] = srcVal;
      }
    }
  }
  return target;
};

ezyadmin.fetchAll = function (fetchFunctions, callback) {
  var targetCountdown = 0;
  var conditionFunc = (fetchFunction) => {
    if (fetchFunction.condition) {
      return fetchFunction.condition();
    }
    return true;
  };
  fetchFunctions.forEach((fetchFunction) => {
    if (conditionFunc(fetchFunction)) {
      ++targetCountdown;
    }
  });
  if (targetCountdown) {
    var countdown = 0;
    var finalCallback = () => {
      if (++countdown == targetCountdown) {
        callback();
      }
    };
    fetchFunctions.forEach((fetchFunction) => {
      if (conditionFunc(fetchFunction)) {
        fetchFunction(finalCallback);
      }
    });
  } else {
    callback();
  }
};

ezyadmin.compareVersions = function (version1, version2) {
  if (!version1 && !version2) {
    return 0;
  }
  if (version1 && !version2) {
    return 1;
  }
  if (!version1) {
    return -1;
  }
  var v1Parts = version1.split(".");
  var v2Parts = version2.split(".");
  var minLength = Math.min(v1Parts.length, v2Parts.length);
  for (var i = 0; i < minLength; i++) {
    var v1Part = parseInt(v1Parts[i]);
    var v2Part = parseInt(v2Parts[i]);
    if (v1Part > v2Part) {
      return 1;
    } else if (v1Part < v2Part) {
      return -1;
    }
  }
  return 0;
};

ezyadmin.extractIframeSrcIfNeed = function (value) {
  var src = value;
  if (value.indexOf("<iframe") >= 0) {
    var regex = /src="([^"]+)"/;
    var match = value.match(regex);
    if (match && match[1]) {
      src = match[1];
    }
  }
  return src;
};

ezyadmin.flattenObject = function (obj, parentKey = "", result = {}) {
  for (var key in obj) {
    if (obj.hasOwnProperty(key)) {
      var propName = parentKey ? `${parentKey}.${key}` : key;
      if (typeof obj[key] === "object" && obj[key] !== null) {
        ezyadmin.flattenObject(obj[key], propName, result);
      } else {
        result[propName] = obj[key];
      }
    }
  }
  return result;
};

ezyadmin.createUrlWithPreviousScreenParams = function (
  uri,
  previousScreen,
  previousQueryString
) {
  var params = [];
  var actualPreviousScreen = previousScreen || ezyadmin.previousScreen;
  if (actualPreviousScreen) {
    params.push("previousScreen=" + actualPreviousScreen);
  }
  var actualPreviousQueryString =
    previousQueryString || ezyadmin.previousQueryString;
  if (actualPreviousQueryString) {
    params.push("queryString=" + encodeURIComponent(actualPreviousQueryString));
  } else if (ezyadmin.lang) {
    params.push("lang=" + ezyadmin.lang);
  }
  return uri + (params.length ? "?" + params.join("&") : "");
};
// ============== component ======================
ezyadmin.shortToast = function (colorClass, title, body) {
  // Create toast container if it doesn't exist
  if (!document.getElementById("ezyadmin-toast-container")) {
    var toastContainer = document.createElement("div");
    toastContainer.id = "ezyadmin-toast-container";
    toastContainer.className = "toast-container position-fixed top-0 end-0 p-3";
    toastContainer.style.zIndex = "9999";
    document.body.appendChild(toastContainer);
  }

  // Create toast element
  var toastElement = document.createElement("div");
  toastElement.className =
    "toast align-items-center text-white border-0 " + colorClass;
  toastElement.setAttribute("role", "alert");
  toastElement.setAttribute("aria-live", "assertive");
  toastElement.setAttribute("aria-atomic", "true");

  // Create toast body
  var toastBody = document.createElement("div");
  toastBody.className = "d-flex";

  var toastContent = document.createElement("div");
  toastContent.className = "toast-body";

  if (title) {
    var titleElement = document.createElement("strong");
    titleElement.textContent = title;
    toastContent.appendChild(titleElement);

    if (body) {
      var br = document.createElement("br");
      toastContent.appendChild(br);
      var bodyText = document.createTextNode(body);
      toastContent.appendChild(bodyText);
    }
  } else {
    var bodyText = document.createTextNode(body || "");
    toastContent.appendChild(bodyText);
  }

  toastBody.appendChild(toastContent);

  // Create close button
  var closeButton = document.createElement("button");
  closeButton.type = "button";
  closeButton.className = "btn-close btn-close-white me-2 m-auto";
  closeButton.setAttribute("data-bs-dismiss", "toast");
  closeButton.setAttribute("aria-label", "Close");
  toastBody.appendChild(closeButton);

  toastElement.appendChild(toastBody);

  // Add to container
  document.getElementById("ezyadmin-toast-container").appendChild(toastElement);

  // Initialize and show toast
  var toast = new bootstrap.Toast(toastElement, {
    autohide: true,
    delay: 2000,
  });
  toast.show();

  // Remove from DOM after hiding
  toastElement.addEventListener("hidden.bs.toast", function () {
    toastElement.remove();
  });
};

ezyadmin.copyToClipboard = function (elementId, isInput, alertTextPrefix) {
  var copiedElement = $("#" + elementId);
  var text = isInput ? copiedElement.val() : copiedElement.text();
  navigator.clipboard.writeText(text.trim());
  ezyadmin.shortToast(
    "bg-success",
    alertTextPrefix + " " + ezyadmin.messages.copied.toLowerCase()
  );
};

ezyadmin.formViewByFormData = new WeakMap();
ezyadmin.formDataToObject = function (formName) {
  var object = {};
  var formData = new FormData(document.querySelector(`#${formName}`));
  formData.forEach((value, key) => {
    if (!Reflect.has(object, key)) {
      object[key] = value;
      return;
    }
    if (!Array.isArray(object[key])) {
      object[key] = [object[key]];
    }
    object[key].push(value);
  });
  var formView = {};
  ezyadmin.formViewByFormData.set(object, formView);
  Object.keys(object).forEach((key) => {
    var id = $(`#${formName} [name="${key}"]`).attr("id") || key;
    formView[id] = key;
  });
  return object;
};

ezyadmin.setInputLengthText = function (inputId) {
  var inputElement = $("#" + inputId);
  var inputValue = inputElement ? inputElement.val() : "";
  inputValue = inputValue || "";
  $("#" + inputId + "Length").text(inputValue.length);
};

ezyadmin.watchInputKeyDown = function (
  elementId,
  maxValueLength,
  onEnterCallback
) {
  var element = $("#" + elementId);
  element.keydown(function () {
    var keyword = $(this).val();
    if (event.key === "Enter") {
      if (onEnterCallback) {
        onEnterCallback();
      }
    } else if (keyword.length > maxValueLength) {
      $(element).val(keyword.substring(0, keyword.length - 1));
    }
  });
  element.keyup(function () {
    $("#" + elementId + "Length").text(
      element.val().length > maxValueLength
        ? maxValueLength
        : element.val().length
    );
  });
};

ezyadmin.formatInputNumber = function (e) {
  $(e).val(ezyadmin.formatNumberWithCommas($(e).val()));
};

ezyadmin.extractMediaUrl = function (media) {
  return media
    ? (media.url || "").trim() ||
        (ezyadmin.accessibleUris["/api/v1/media/{name}"] == false
          ? "/api/v1/me/media/"
          : "/api/v1/media/") + media.name
    : "";
};

ezyadmin.scrollToElement = function (elementId, duration) {
  var actualElementId = elementId.startsWith("#") ? elementId : "#" + elementId;
  if (duration) {
    var element = $(actualElementId);
    if (element) {
      var offset = element.offset();
      if (offset) {
        $("html, body").animate(
          {
            scrollTop: offset.top,
          },
          duration
        );
      }
    }
  } else {
    document.querySelector(actualElementId).scrollIntoView();
  }
};

// ============== json ======================
ezyadmin.syntaxHighlightJson = function (json) {
  if (typeof json != "string") {
    json = JSON.stringify(json, undefined, 2);
  }
  json = json
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
  return json.replace(
    /("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g,
    function (match) {
      var cls = "number";
      if (/^"/.test(match)) {
        if (/:$/.test(match)) {
          cls = "key";
        } else {
          cls = "string";
        }
      } else if (/true|false/.test(match)) {
        cls = "boolean";
      } else if (/null/.test(match)) {
        cls = "null";
      }
      return '<span class="' + cls + '">' + match + "</span>";
    }
  );
};

// ============== bytes ======================
var KB_SIZE = 1024;
var MB_SIZE = 1024 * KB_SIZE;
var GB_SIZE = 1024 * MB_SIZE;
var TB_SIZE = 1024 * GB_SIZE;
ezyadmin.toCapacityString = function (input) {
  var tb = (input / TB_SIZE) | 0;
  if (tb) {
    return (((input / TB_SIZE) * 10) | 0) / 10 + "TB";
  }
  var gb = (input / GB_SIZE) | 0;
  if (gb) {
    return (((input / GB_SIZE) * 10) | 0) / 10 + "GB";
  }
  var mb = (input / MB_SIZE) | 0;
  if (mb) {
    return (((input / MB_SIZE) * 10) | 0) / 10 + "MB";
  }
  var kb = (input / KB_SIZE) | 0;
  if (kb) {
    return (((input / KB_SIZE) * 10) | 0) / 10 + "KB";
  }
  return input + "B";
};

// ============== metrics ======================
var THOUSAND = 1000;
var MILLION = 1000 * THOUSAND;
var BILLION = 1000 * MILLION;
var TRILLION = 1000 * BILLION;
ezyadmin.toCountString = function (input) {
  var tb = (input / TRILLION) | 0;
  if (tb) {
    return (((input / TRILLION) * 10) | 0) / 10 + "T";
  }
  var gb = (input / BILLION) | 0;
  if (gb) {
    return (((input / BILLION) * 10) | 0) / 10 + "B";
  }
  var mb = (input / MILLION) | 0;
  if (mb) {
    return (((input / MILLION) * 10) | 0) / 10 + "M";
  }
  var kb = (input / THOUSAND) | 0;
  if (kb) {
    return (((input / THOUSAND) * 10) | 0) / 10 + "K";
  }
  return input;
};

// ==================== cookie ===========
ezyadmin.setCookie = function (name, value, days) {
  var expires = "";
  if (days) {
    var date = new Date();
    date.setTime(date.getTime() + days * 24 * 60 * 60 * 1000);
    expires = "; expires=" + date.toUTCString();
  }
  document.cookie = name + "=" + value + expires + "; path=/";
};

ezyadmin.deleteCookie = function (name) {
  document.cookie = name + "=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
};

ezyadmin.getCookie = function (cname) {
  var cookies = document.cookie.split("; ");
  for (var i = 0; i < cookies.length; ++i) {
    var equalIndex = cookies[i].indexOf("=");
    var keyValue = [
      cookies[i].substring(0, equalIndex).trim(),
      cookies[i].substring(equalIndex + 1).trim(),
    ];
    if (keyValue[0] == cname && keyValue[1]) {
      return keyValue[1];
    }
  }
  return "";
};

ezyadmin.getCookieAccessToken = function () {
  return ezyadmin.getCookie("adminAccessToken");
};

// ============== management ======================
ezyadmin.enableServer = function (target) {
  ezyadmin.doTargetAction(target, "Enable", false);
};

ezyadmin.disableServer = function (target) {
  ezyadmin.doTargetAction(target, "Disable", false);
};

ezyadmin.startServer = function (target) {
  ezyadmin.doTargetAction(target, "Start", true);
};

ezyadmin.stopServer = function (target) {
  ezyadmin.doTargetAction(target, "Stop", false);
};

ezyadmin.restartServer = function (target) {
  ezyadmin.doTargetAction(target, "Restart", true);
};

ezyadmin.doTargetAction = function (target, action, showLoading) {
  if (ezyadmin.stopPing) {
    ezyadmin.stopPing(target);
  }
  if (showLoading) {
    ezyadmin.showLoadingScreen();
  }
  if (target == "socket") {
    if (ezyadmin.socket) {
      ezyadmin.socket.disconnect();
    }
  }
  var actionLowerCase = action.toLowerCase();
  $.ajax({
    url: "/api/v1/" + actionLowerCase + "?target=" + target,
    type: "POST",
  })
    .done(function (data) {
      ezyadmin.shortToast(
        "bg-success",
        ezyadmin.messages[actionLowerCase] +
          " " +
          ezyadmin.messages.successfully.toLowerCase()
      );
      if (actionLowerCase != "stop") {
        if (ezyadmin.startPingInterval) {
          setTimeout(ezyadmin.startPingInterval, 6000);
        }
        if (actionLowerCase == "enable" || actionLowerCase == "disable") {
          location.reload();
        } else if (target == "socket") {
          if (actionLowerCase == "start") {
            ezyadmin.socket.connect();
          } else {
            setTimeout(ezyadmin.socket.connect, 6000);
          }
        }
      }
    })
    .fail(function (e) {
      ezyadmin.processUpdateApiErrors({}, e);
      ezyadmin.shortToast(
        "bg-danger",
        ezyadmin.messages[actionLowerCase] +
          " " +
          ezyadmin.messages.failed.toLowerCase()
      );
    });
};

// ============== module ======================
ezyadmin.ensureAdminRunning = function (callback) {
  var retryTime = 0;
  var maxRetryTime = 120;
  var ensureAdminRunningPing = function () {
    $.ajax({
      url: "/management/health-check",
      type: "GET",
    })
      .done(function (data) {
        callback();
      })
      .fail(function (e) {
        ezyadmin.processGetApiErrors(e);
        retryTime++;
        if (retryTime <= maxRetryTime) {
          setTimeout(ensureAdminRunningPing, 1000);
        } else {
          callback();
        }
      });
  };
  setTimeout(() => {
    ensureAdminRunningPing();
  }, 9000);
};

// ============== csv ======================
ezyadmin.onCsvFileChanged = function (input, callback, includeFirstLine) {
  var file = input.files[0];
  if (file) {
    var reader = new FileReader();
    reader.onload = function (e) {
      var content = e.target.result;
      var rowsList = ezyadmin.parseCsvContent(content);
      callback(file, rowsList);
    };
    reader.readAsText(file);
  }
};

ezyadmin.parseCsvContent = function (content, includeFirstLine) {
  var lines = content.split("\n");
  var rows = [];
  if (lines.length) {
    var firstLine = lines[0];
    var commas = firstLine.indexOf(",") >= 0 ? "," : ";";
    if (!includeFirstLine) {
      lines = lines.slice(1);
    }
    lines.forEach((line) => {
      rows.push(ezyadmin.readCsvLine(line, commas));
    });
  }
  return rows;
};

ezyadmin.readCsvLine = function (line, commas) {
  var cells = [];
  var cell = "";
  var strs = line.split(commas);
  for (var i = 0; i < strs.length; ++i) {
    var str = strs[i].trim();
    if (cell.startsWith('"')) {
      if (str.endsWith('"')) {
        cell += strs[i].slice(0, strs[i].lastIndexOf('"') + 1);
        cell = cell.slice(1, cell.length - 1);
        cells.push(cell.trim());
        cell = "";
      } else {
        cell += strs[i] + commas;
      }
    } else {
      if (str.startsWith('"')) {
        cell = strs[i].slice(strs[i].indexOf('"'));
      } else {
        cells.push(str);
        cell = "";
      }
    }
  }
  return cells;
};
