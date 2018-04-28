#include <io.h>
#include <stdio.h>
#include <conio.h>
#include <memory.h>

#include "stdafx.h"
#include <math.h>
#define LN2 0.693147180559945309417232121458177




double Entropy(unsigned char* data,int dataSize)
{
	unsigned int		numr[256];
	double				dS = 0;
	unsigned int		N;
	unsigned long int	NumBytes=0;
	unsigned long int	Num256Read = 0;
	double				dbyte;
	double				Ent = 0;
	unsigned char		*buf;
	
	memset(numr, 0, sizeof numr);


	int bufIndex = 0;

	while(dataSize)
	{
		buf = data+bufIndex;
		if(dataSize > 256)
		{
			NumBytes = 256;
			dataSize -= 256;
		}
		else
		{
			NumBytes = dataSize;
			dataSize = 0;
		}

		
		dS = 0;

		for (N = 0; N < NumBytes; ++N)  ++numr[buf[N]];
	
		for (N = 0; N < 256; ++N)
		{
			if (numr[N])
			{	

				dbyte = double(numr[N]) / 256;
				
				dS -= dbyte * log(dbyte) / LN2;
				

				numr[N] = 0;
			};
		};

		++Num256Read;
		

		Ent += dS;
		bufIndex += NumBytes;


	};

	return Ent / Num256Read;
};