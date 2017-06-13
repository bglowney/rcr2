grammar Rcr2;

statement : (assignment | sideeffect) WS? ';' EOF;
assignment : alias WS? '=' WS? expression ;
alias: USER_WORD;
sideeffect : expression ;
expression : function (WS arggroup)? ;
arggroup: userArgGroup | topLevelArgGroup;
userArgGroup: arg (WS arg)*;
topLevelArgGroup: '(' arg? (',' WS? arg)* ')';
function: USER_WORD;
arg: USER_WORD | expression;
WS : [ \n\t]+;
USER_WORD : ~[ \n\t;,()]+;