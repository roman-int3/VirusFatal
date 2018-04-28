#pragma once

template<typename T> class CProcessData
{
public:	

	CProcessData(DWORD dwProcessId = 0, DWORD dwDesiredAccess = PROCESS_ALL_ACCESS,
		DWORD flAllocationType = MEM_COMMIT, DWORD flProtect = PAGE_READWRITE)
	{
		m_hProcess = OpenProcess(dwDesiredAccess, FALSE, 
			dwProcessId ? dwProcessId : GetCurrentProcessId());

		if(m_hProcess)
		{
			m_lpData = VirtualAllocEx(m_hProcess, NULL, sizeof T, 
				flAllocationType, flProtect);

		}
	}

	~CProcessData()
	{
		if(m_hProcess)
		{			
			if(m_lpData)
			{
				VirtualFreeEx(m_hProcess, m_lpData, NULL, MEM_RELEASE);
			}
			CloseHandle(m_hProcess);
		}
	}

	BOOL WriteData(const T& data)
	{
		return (m_hProcess && m_lpData) ? WriteProcessMemory(m_hProcess, m_lpData, 
			(LPCVOID)&data, sizeof T, NULL) : FALSE;
	}


	BOOL ReadData(T* data)
	{
		return (m_hProcess && m_lpData) ? ReadProcessMemory(m_hProcess, m_lpData, 
			(LPVOID)data, sizeof T, NULL) : FALSE;
	}


	template<typename TSUBTYPE> BOOL ReadData(TSUBTYPE* data, LPCVOID lpData)
	{
		return m_hProcess ? ReadProcessMemory(m_hProcess, lpData, 
			(LPVOID)data, sizeof TSUBTYPE, NULL) : FALSE;
	}


	const T* GetData()
	{
		return (m_hProcess && m_lpData) ? (T*)m_lpData : NULL;
	}
private:
	HANDLE m_hProcess;
	LPVOID m_lpData;
};
