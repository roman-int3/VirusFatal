
#include "Array.h"

#ifndef __myArraySort
#define __myArraySort


class myArraySort : public myArray
{
public:

	BOOL Add(word_info *data);
	void q_sort(myArray *pArr,int left,int right);
};

#endif