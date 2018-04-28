
#include <Windows.h>
#include <ole2.h> // IDataObject
#include <shlobj.h> // DROPFILES

char szID_IDListArray[] = "Shell IDList Array";

class CDataObject : public IDataObject, IEnumFORMATETC
{
  // Members

protected:
  BOOL m_bReset;
  WCHAR *m_szFiles;
  int m_nLen;
  
  UINT nGlobal_IDListFmt;

    // Constructor

public:
  CDataObject(WCHAR* szFiles)
  {
    Reset();
 nGlobal_IDListFmt = RegisterClipboardFormatA( szID_IDListArray );
    if (!szFiles)
    {
      m_szFiles = NULL;
      return;
    }

    // replace \n chars with \0 chars

    m_nLen = lstrlenW(szFiles)*sizeof(WCHAR)+2;
    m_szFiles = new WCHAR[m_nLen];
    memcpy(m_szFiles, szFiles, m_nLen);

    WCHAR* szTmp = m_szFiles;
    while ( szTmp=wcschr(szTmp,'\n') )
      *szTmp++ = '\0';

  }

  virtual ~CDataObject()
  {
    delete [] m_szFiles;
  }

public:
  HRESULT __stdcall QueryInterface(REFIID iid, void** ppvObject)
  {
    *ppvObject = (IDataObject*) this;
    return S_OK;
  }

  ULONG __stdcall AddRef()
  {
    return 1;
  }
  ULONG __stdcall Release()
  {
    return 0;
  }



ITEMIDLIST *PathToPidl( IShellFolder *AFolder)
{
	ITEMIDLIST *Result = 0;
  HRESULT hr;
  IShellFolder* oDesktop, *oFolder ;
  DWORD nCharsParsed, nAttr;

		hr = SHGetDesktopFolder( &oDesktop );
		if(hr == S_OK )
		{
			if( AFolder ) oFolder = AFolder ;
			else oFolder = oDesktop;

			hr = oFolder->ParseDisplayName( 0, NULL, m_szFiles, &nCharsParsed, &Result, &nAttr );
			if( hr != S_OK ) Result = NULL;
		}


	 return Result;
}

typedef struct TCIdaRec
{
    int nDim;                             // The number of PIDLs that are being transferred, not including the parent folder
    int ofsParent;                        // offset to the fully-qualified PIDL of a parent folder
    int ofsItem;                          //
    ITEMIDLIST *lpParent;                  // PItemIDList of parent folder
};

int GetSizeToDropIDList()
{
	ITEMIDLIST *lpItem;
	IMalloc *oShellMalloc ;
	int Result = 0;
     // http://msdn.microsoft.com/en-us/library/bb776902.aspx#CFSTR_SHELLIDLIST
     
    lpItem = PathToPidl( NULL);
    if(lpItem)
	{
		Result = sizeof( TCIdaRec ) + ILGetSize( lpItem );
		SHGetMalloc( &oShellMalloc );
        oShellMalloc->Free( lpItem );
		oShellMalloc->Release();
	}

	return Result;
}



int GetAllocSize(CLIPFORMAT cfFormat)
{
 

    if( CF_HDROP == cfFormat )
	{
          return sizeof( DROPFILES ) + m_nLen;
	}
	else if( nGlobal_IDListFmt = cfFormat )
	{
          return GetSizeToDropIDList();
	}

	return 0;
}




void DumpDropFilesToMemory(void *AMem)
{
	int nOffset;
	LPDROPFILES pDropFiles = (LPDROPFILES)AMem;

    nOffset = sizeof(DROPFILES);
    pDropFiles->pFiles = nOffset;
    pDropFiles->fWide = TRUE;
    
    RtlMoveMemory((char*)((DWORD)AMem+nOffset), this->m_szFiles, this->m_nLen );
}

void DumpIDListArrayToMemory(void* AMem)
{
	IMalloc *oShellMalloc ;
	DWORD nOffset;
	ITEMIDLIST *lpItem;
	TCIdaRec *Rec = (TCIdaRec*)AMem;

	lpItem = PathToPidl( NULL);
    if( !lpItem ) return; 
	 
	nOffset = 3 * sizeof( UINT );
	Rec->nDim = 1;
    Rec->ofsParent = nOffset;
    Rec->ofsItem = nOffset + sizeof(DWORD);
    Rec->lpParent = NULL;
    MoveMemory((void*)((DWORD)AMem + Rec->ofsItem), lpItem, ILGetSize( lpItem ) );
    SHGetMalloc( &oShellMalloc );
    oShellMalloc->Free( lpItem );
	oShellMalloc->Release();
}


  HRESULT __stdcall GetData(FORMATETC* pFormatetc, STGMEDIUM* pmedium)
  {
    if (pFormatetc->tymed != TYMED_HGLOBAL)
      return DV_E_FORMATETC;

    if (!m_szFiles)
      return S_FALSE;

	ZeroMemory( pmedium,sizeof(STGMEDIUM));
	int nSize = GetAllocSize(pFormatetc->cfFormat);
    if( nSize <= 0 ) return DV_E_CLIPFORMAT;

	HGLOBAL hMem = GlobalAlloc(GMEM_FIXED | GMEM_ZEROINIT, nSize );
    void *lp = GlobalLock( hMem );
    if(!lp) return STG_E_MEDIUMFULL;
	if(pFormatetc->cfFormat == CF_HDROP)
          DumpDropFilesToMemory( lp ); 
	else
          DumpIDListArrayToMemory( lp );
	
     GlobalUnlock( hMem );
      pmedium->tymed = TYMED_HGLOBAL;
      pmedium->hGlobal = hMem;
     

    return S_OK;
  }

  HRESULT __stdcall GetDataHere(FORMATETC* pFormatetc, STGMEDIUM* pmedium)
  {
	  return E_NOTIMPL;
  }

  HRESULT __stdcall QueryGetData(FORMATETC* pFormatetc)
  {
    return S_OK;
  }

  HRESULT __stdcall GetCanonicalFormatEtc(FORMATETC* pFormatetcIn, 
                                          FORMATETC* pFormatetcOut)
  {
    return E_NOTIMPL;
  }

  HRESULT __stdcall SetData(FORMATETC* pFormatetc, 
                   STGMEDIUM* pmedium, BOOL fRelease)
  {
    return E_NOTIMPL;
  }

  HRESULT __stdcall EnumFormatEtc(DWORD dwDirection, 
                        IEnumFORMATETC** ppenumFormatetc)
  {

      return E_NOTIMPL;
  }

  HRESULT __stdcall DAdvise(FORMATETC* pFormatetc, 
                            DWORD advf, 
                            IAdviseSink* pAdvSink, 
                            DWORD* pdwConnection)
  {
    return OLE_E_ADVISENOTSUPPORTED;
  }

  HRESULT __stdcall DUnadvise(DWORD dwConnection)
  {
    return OLE_E_ADVISENOTSUPPORTED;
  }

  HRESULT __stdcall EnumDAdvise(IEnumSTATDATA** ppenumAdvise)
  {
    return OLE_E_ADVISENOTSUPPORTED;
  }













  // IEnumFORMATETC implementation

  //

  HRESULT __stdcall Next( 
            /*[in]*/ ULONG celt,
            /*[out]*/ FORMATETC __RPC_FAR* rgelt,
            /*[out]*/ ULONG __RPC_FAR* pceltFetched)
  {
    if (!m_bReset) return S_FALSE;

    m_bReset = FALSE;

    FORMATETC fmt;
    fmt.cfFormat = CF_HDROP;
    fmt.dwAspect = DVASPECT_CONTENT;
    fmt.lindex = -1;
    fmt.ptd = NULL;
    fmt.tymed = TYMED_HGLOBAL;
    *rgelt = fmt; // copy struct

    if (pceltFetched) *pceltFetched = 1;

    return S_OK;
  }
        
  HRESULT __stdcall Skip(/*[in]*/ ULONG celt)
  {
    return S_FALSE;
  }
        
  HRESULT __stdcall Reset()
  {
    m_bReset = TRUE;
    return S_OK;
  }
        
  HRESULT __stdcall Clone( 
            /* [out] */ IEnumFORMATETC** ppenum)
  {
    return S_OK;
  }
};