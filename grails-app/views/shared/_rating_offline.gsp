<!-- BEGIN: RATING_OFFLINE -->
<%
  def rating = artifactInstance.averageRating ?: 4i
  List ratings = (1..5).collect([]) { i ->
    float remainder = i - rating
    if (rating >= i) {
      [css: 'star on', style: '']
    } else if ((remainder > 0i) && (remainder < 1i)) {
      [css: 'star on', style: 'width: ' + (1i - remainder) * 100i + '%']
    } else {
      [css: 'star', style: '']
    }
  }
%>
<div class="artifact-ratings ratings">
  <table class="ratingDisplay">
    <tr>
      <g:each in="${ratings}" var="props">
        <td><div class="${props.css}">
          <g:if test="${login}">
            <a href="${application.contextPath}/signin?originalURI=${request.forwardURI - application.contextPath}"
               style="${props.style}">a</a>
          </g:if>
          <g:else>
            <a style="${props.style}">a</a>
          </g:else>
        </div></td>
      </g:each>
      <td>(${artifactInstance.totalRatings})</td>
    </tr>
  </table>
</div>
<!-- END: RATING_OFFLINE -->