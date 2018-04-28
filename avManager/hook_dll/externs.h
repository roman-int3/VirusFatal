

extern pTextOutW p_TextOutWProc;
extern pTextOutW p_TextOutWOldProc;
extern pDrawTextW p_DrawTextWProc;
extern pDrawTextW p_DrawTextWOldProc;
extern pExtTextOutW p_ExtTextOutWProc;
extern pExtTextOutW p_ExtTextOutWOldProc;
extern pExtTextOutA p_ExtTextOutAProc;
extern pExtTextOutA p_ExtTextOutAOldProc;
extern pDrawTextExW p_DrawTextExWProc;
extern pDrawTextExW p_DrawTextExWOldProc;
extern pGetTextExtentPointW p_GetTextExtentPointWProc;
extern pGetTextExtentPointW p_GetTextExtentPointWOldProc;

extern CRITICAL_SECTION grabDataSection;
extern char *szTextData;
extern myArray wordsArr;
extern myArray wordsInfoArr;
extern BOOL  hashstr;
extern BOOL compiletostringline;

