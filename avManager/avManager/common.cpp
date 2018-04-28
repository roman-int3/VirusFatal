
#include "stdafx.h"
#include "externs.h"


void* MyRealloc(void *buf,int len)
{
	HANDLE hProcHeap = GetProcessHeap();
	if(buf == 0)
		return (void *)HeapAlloc(hProcHeap,HEAP_ZERO_MEMORY,len+1);
	return (void *)HeapReAlloc(hProcHeap,HEAP_ZERO_MEMORY,buf,HeapSize(hProcHeap,0,buf)+len);
}


void MyFree(void *buf)
{
	if(buf != 0)HeapFree(GetProcessHeap(),HEAP_NO_SERIALIZE,buf);
}


WCHAR* CharToWChar(char* szString)
{
	int Size = lstrlen(szString)+2;
	WCHAR* wData = (WCHAR*)GlobalAlloc(GPTR,Size*2);
	MultiByteToWideChar(CP_ACP,0,szString,-1,wData,Size);
	return wData;
}


char* WCharToChar(WCHAR* wString)
{
	int Size = lstrlenW(wString)+1;
	char* wData = (char*)GlobalAlloc(GPTR,Size);
	WideCharToMultiByte(CP_ACP,0,wString,-1,wData,Size,0,0);
	return wData;
}


int getFileMap(WCHAR* sFileName,HANDLE *hFile,HANDLE *hMap,void** hView)
{
	*hFile = CreateFileW(sFileName,GENERIC_READ,FILE_SHARE_READ,0,OPEN_EXISTING,FILE_ATTRIBUTE_NORMAL,0);	
	if(*hFile == INVALID_HANDLE_VALUE)
	{
		return 0;	
	}
	*hMap = CreateFileMapping(*hFile,0,PAGE_READONLY,0,0,0);
	if(*hMap == 0)
	{
		CloseHandle(*hFile);
		return 0;
	}
	*hView = MapViewOfFile(*hMap,FILE_MAP_READ,0,0,0);
	if(*hView == 0)
	{
		CloseHandle(*hMap);
		CloseHandle(*hFile);
		return 0;
	}
	return GetFileSize(*hFile,0);
}


void closeFileMap(HANDLE hFile,HANDLE hMap,void* hView)
{
	UnmapViewOfFile(hView);
	CloseHandle(hMap);
	CloseHandle(hFile);
}


BOOL AdjustPrivileges(char *pPriv, BOOL add)
{
	BOOL bRet = FALSE;
	TOKEN_PRIVILEGES tkp;
 	HANDLE hToken;

	if (!OpenProcessToken(GetCurrentProcess(),TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY,&hToken))
		return bRet;

	if (!LookupPrivilegeValue(NULL, pPriv, &tkp.Privileges[0].Luid)) {
		CloseHandle(hToken);
		return bRet;
	}

	tkp.PrivilegeCount = 1; 
	if (add)
		tkp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;
	else 
		tkp.Privileges[0].Attributes ^= (SE_PRIVILEGE_ENABLED & tkp.Privileges[0].Attributes);

	bRet = AdjustTokenPrivileges(hToken,FALSE,&tkp,sizeof(TOKEN_PRIVILEGES), (PTOKEN_PRIVILEGES) NULL, 0);
	
	CloseHandle(hToken);

	return bRet;
}


int ChLastIndex(char *szStr,char ch)
{
	int strCount = lstrlen(szStr);
	szStr += strCount;
	for(;strCount;strCount--,szStr--)
	{
		if(szStr[0] == ch) return strCount; 
	}

	return -1;
}


char* GetFilenameFromPid(DWORD pid)
{
	char szBuf[512];
	char *strRet = 0;

	HANDLE ph = OpenProcess(PROCESS_QUERY_INFORMATION,FALSE,pid);
	if(ph)
	{
		int strLength = GetProcessImageFileName(ph,szBuf, 512);
		if(strLength)
		{
			strRet = (char*)GlobalAlloc(GPTR,strLength+1);
			int index = ChLastIndex(szBuf,'\\');
			RtlMoveMemory(strRet,szBuf+index,strLength-index);
		}
		
		CloseHandle(ph);
	}
	return strRet;
}



BOOL CALLBACK EnumGetChildWinByID(HWND hwnd,LPARAM lParam)
{
	SEND_MSG_PARAM *mp = (SEND_MSG_PARAM*)lParam;
	long curID = GetWindowLong(hwnd,GWL_ID);

	if(curID == mp->elemID)
	{
		mp->elemHwnd = hwnd;
		return false;
	}

	return true;
}


BOOL CALLBACK EnumGetRootHwndWindowsProc(HWND hwnd,LPARAM lParam)
{
	char lpString[512];
	DWORD lngProcessID;
	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO*)lParam;
	
	GetWindowThreadProcessId(hwnd,&lngProcessID);
	if(spi->szMainWinClass)
	{
		if(spi->pi.dwProcessId && lngProcessID == spi->pi.dwProcessId || !spi->pi.dwProcessId)
		{
			GetClassName(hwnd,lpString,512);
			if(StrCmpI(lpString,spi->szMainWinClass) == 0)
			{
				spi->rootHwnd = hwnd;
				spi->pi.dwProcessId = lngProcessID;
				return false;
			}
		}
	}

	if(spi->szMainWinTitle)
	{
		if(spi->pi.dwProcessId && lngProcessID == spi->pi.dwProcessId || !spi->pi.dwProcessId)
		{
			GetWindowText(hwnd,lpString,512);
			if(StrStrI(lpString,spi->szMainWinTitle))
			{
				spi->rootHwnd = hwnd;
				spi->pi.dwProcessId = lngProcessID;
				return false;
			}
		}
	}

	

	return true;
}


BOOL CALLBACK sendMsgEnumParentWindowsProc(HWND hwnd,LPARAM lParam)
{
	DWORD lngProcessID;
	SEND_MSG_PARAM *mp = (SEND_MSG_PARAM*)lParam;
	DWORD result;
	char szWndInfoBuf[500];
	
	GetWindowThreadProcessId(hwnd,&lngProcessID);
	
	
	if(lngProcessID == mp->spi->pi.dwProcessId)
	{
		if(mp->spi->rootHwnd == 0)
		{
			DebugLog("sendMsgEnumParentWindowsProc: mp->rootHwnd == 0");
			EnumWindows(&EnumGetRootHwndWindowsProc,(LPARAM)mp->spi);
			DebugLog((int)mp->spi->rootHwnd);

		}

		if(mp->elemName == 0 && mp->elemID == 0 && mp->className == 0)
		{
			SendMessage(mp->spi->rootHwnd,mp->msg,mp->wParam,mp->lParam);
			mp->Result = true;
			DebugLog("sendMsgEnumParentWindowsProc: message send to root hwnd: ");
			DebugLog((int)mp->spi->rootHwnd);
			return false;
		}

		EnumChildWindows(hwnd,sendMsgEnumParentWindowsProc,lParam);
		if(mp->Result) return false;
  
		if(mp->elemName)
		{
			szWndInfoBuf[0] = 0;
			GetWindowText(hwnd,szWndInfoBuf,500);

			if(StrCmpI(mp->elemName,szWndInfoBuf) == 0)
			{
				if(IsWindowEnabled(hwnd) && IsWindowVisible(hwnd))
				{
					if(mp->msg == BM_CLICK)
					{
						RECT rc;
						INPUT inp;
						RtlZeroMemory(&inp,sizeof(INPUT));
						inp.type = INPUT_MOUSE;
						inp.mi.dwFlags = MOUSEEVENTF_ABSOLUTE|MOUSEEVENTF_LEFTDOWN|MOUSEEVENTF_LEFTUP;

						GetWindowRect(hwnd,&rc);
						rc.left+=2,rc.top+=2;
						SetCursorPos(rc.left,rc.top);
						BringWindowToTop(mp->spi->rootHwnd);
						SetForegroundWindow(mp->spi->rootHwnd);
						inp.mi.dx = rc.left;
						inp.mi.dy = rc.top;

						UINT numEvents = SendInput(1,&inp,sizeof(INPUT));
						if(numEvents == 0)
						{
							LPVOID lpMsgBuf;

							FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM|FORMAT_MESSAGE_ALLOCATE_BUFFER| FORMAT_MESSAGE_IGNORE_INSERTS,
								NULL,
								GetLastError(),
								NULL,
								(LPTSTR) &lpMsgBuf,
								0,
								NULL);
							ErrorLog("SendInput ERROR:");
							ErrorLog((char*)lpMsgBuf);

							LocalFree( lpMsgBuf );
						}
					}
					else
					{
						SendMessage(hwnd,mp->msg,mp->wParam,mp->lParam);
					}
					mp->Result = true;
					DebugLog("clickEnumParentWindowsProc::Message send.");
					return false;
				}
			}
		}
		else if(mp->className) 
		{
			szWndInfoBuf[0] = 0;
			GetClassName(hwnd,szWndInfoBuf,500);

			if(StrCmpI(mp->className,szWndInfoBuf) == 0)
			{
				if(IsWindowEnabled(hwnd) && IsWindowVisible(hwnd))
				{
					SetFocus(hwnd);
					SendMessage(hwnd,mp->msg,mp->wParam,mp->lParam);
					mp->Result = true;
					DebugLog("clickEnumParentWindowsProc::Message send.");
					return false;
				}
			}
		}
		else if(mp->elemID && mp->elemID == GetWindowLong(hwnd,GWL_ID))
		{
			if(IsWindowVisible(hwnd) && IsWindowEnabled(hwnd)) 
			{
				SetFocus(hwnd);
				SendMessage(hwnd,mp->msg,mp->wParam,mp->lParam);
				mp->Result = true;
				DebugLog("sendMsgEnumParentWindowsProc::Message send.");
				return false;
			}
		}
	}

	return true;
}


BOOL CALLBACK getMainHwndWindowsProc(HWND hwnd,LPARAM lParam)
{

	SCAN_PROCESS_INFO *spi = (SCAN_PROCESS_INFO*)lParam;
	char szBuff[512];
	
	int lenght = GetWindowText(hwnd,szBuff,512);
	if(lenght > 0)
	{
		if(StrStrI(szBuff,spi->szMainWinTitle) != NULL) 
		{
			spi->rootHwnd = hwnd;
			return false;
		}
	}

	return true;
}





void writeLog(char *Data,int dataSize)
{
	DWORD rez;
	SYSTEMTIME st;
	char sz_buf[512];

    GetLocalTime(&st);

		WriteFile(hLogFile,sz_buf,wsprintf(sz_buf,"%02d/%02d/%04d %02d:%02d:%02d\r\n", st.wMonth, st.wDay, st.wYear, st.wHour, st.wMinute, st.wSecond),&rez,0);
		WriteFile(hLogFile,Data,dataSize,&rez,0);
		WriteFile(hLogFile,"\r\n",2,&rez,0);
}

void DebugLog(char *Data)
{
#ifdef _DEBUG
	int dataSize = lstrlen(Data);
	writeLog(Data,dataSize);
#endif
}

void DebugLog(WCHAR *Data,int dataSize)
{
#ifdef _DEBUG
	writeLog((char*)Data,dataSize);
#endif
}

void DebugLog(int Data)
{
#ifdef _DEBUG
	char buf[50];
	int dataSize = wsprintf(buf,"0x%X",Data);
	writeLog(buf,dataSize);
#endif
}


void ErrorLog(WCHAR *Data,int dataSize)
{
	writeLog((char*)Data,dataSize);
}

void ErrorLog(char *Data)
{
	int dataSize = lstrlen(Data);
	writeLog(Data,dataSize);
}

void ErrorLog(int Data)
{
	char buf[50];
	int dataSize = wsprintf(buf,"0x%X",Data);
	writeLog(buf,dataSize);
}