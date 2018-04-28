
#pragma once
#include "CProcessData.h"

struct TRAYDATA
{
	HWND hwnd;				
	UINT uID;				
	UINT uCallbackMessage;	
	DWORD Reserved[2];		
	HICON hIcon;				
};

struct localbuf
{
	char buf[0x1000];
};

char* GetFilenameFromPid(DWORD pid);

class CTray
{
private:
	CProcessData <TBBUTTON> *pData;
	CProcessData <localbuf> *pBufData;
	TBBUTTON tbbtn;
	TRAYDATA tdata;
	localbuf buf;
	HWND trayHwnd;
	DWORD trayProcessID;

	BOOL findTrayBtn(char *pattern,char *szFileName=0)
	{
		char* fileName = 0;
		DWORD dwProcessId;

		int btnCount = SendMessage(trayHwnd,TB_BUTTONCOUNT,0,0);
		for(int i=btnCount;i; i--)
		{
			if(fileName) GlobalFree(fileName);
			SendMessage(trayHwnd,TB_GETBUTTON ,i,(LPARAM)pData->GetData());
			pData->ReadData(&tbbtn);
			pData->ReadData<TRAYDATA>(&tdata,(void*)tbbtn.dwData);
		
			SendMessage(trayHwnd,TB_GETBUTTONTEXT ,tbbtn.idCommand,(LPARAM)pBufData->GetData());
			pBufData->ReadData(&buf);

			GetWindowThreadProcessId(tdata.hwnd,&dwProcessId);
			fileName = GetFilenameFromPid(dwProcessId);
			if(fileName != 0 && szFileName != 0) 
			{ 
				if(StrCmpI(szFileName,fileName) != 0)
				{
					continue;
				}
			}

			if(StrStrI(buf.buf,pattern))
			{
				GlobalFree(fileName);
				return true;
			}
		}
	
		return false;
	}

public:

	CTray()
	{
		HWND hWnd = FindWindow("Shell_TrayWnd", NULL);
		if(hWnd)
		{
			hWnd = FindWindowEx(hWnd,NULL,"TrayNotifyWnd", NULL);
			if(hWnd)
			{
				hWnd = FindWindowEx(hWnd,NULL,"SysPager", NULL);
				if(hWnd)
				{					
					trayHwnd = FindWindowEx(hWnd, NULL,"ToolbarWindow32", NULL);
					DWORD threadId = GetWindowThreadProcessId(trayHwnd,&trayProcessID);
					pData = new CProcessData<TBBUTTON>(trayProcessID);
					pBufData = new CProcessData<localbuf>(trayProcessID);
				}
			}
		}
	}

	char *getTooltipText(char *pattern,char *fileName=0)
	{
		if(findTrayBtn(pattern,fileName))
		{
			int lenght = lstrlen(buf.buf);
			char *retBuf = (char*)GlobalAlloc(GPTR,lenght+1);
			RtlMoveMemory(retBuf,buf.buf,lenght);
			return retBuf;
		}

		return 0;
	}

	BOOL DblClick(char *pattern,char *fileName=0)
	{
		if(findTrayBtn(pattern,fileName))
		{
			return PostMessage(tdata.hwnd,tdata.uCallbackMessage ,tdata.uID,(LPARAM)WM_LBUTTONDBLCLK);
		}

		return FALSE;
	}

	BOOL trackPopupMenu(char *pattern,char *fileName=0)
	{
		if(findTrayBtn(pattern,fileName))
		{
			PostMessage(tdata.hwnd,tdata.uCallbackMessage ,tdata.uID,(LPARAM)WM_RBUTTONDOWN);
			PostMessage(tdata.hwnd,tdata.uCallbackMessage ,tdata.uID,(LPARAM)WM_RBUTTONUP);
		}

		return FALSE;
	}



};
