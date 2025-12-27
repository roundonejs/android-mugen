#ifndef CONFIG_WORKAROUND_HPP
#define CONFIG_WORKAROUND_HPP

class DOS_Drive;
class localDrive;
class fatDrive;
class DOS_File;
class localFile;
class Section;
class Property;

bool dynamic_check(localFile *u);
bool dynamic_check(localDrive *u);
bool dynamic_check(fatDrive *u);

#ifndef NULL
#define NULL 0
#endif

#define dynamic_cast	my_dynamic_cast

template<class T>
T my_dynamic_cast(DOS_Drive *u) {
	if (dynamic_check(static_cast<T>(u)))
		return static_cast<T>(u);
	else
		return NULL;
}

template<class T>
T my_dynamic_cast(Section *u) {
	return static_cast<T>(u);
}

template<class T>
T my_dynamic_cast(Property *u) {
	return static_cast<T>(u);
}



template<class T>
T my_dynamic_cast(DOS_File *u) {
	if (dynamic_check(static_cast<T>(u)))
		return static_cast<T>(u);
	else
		return NULL;
}

#endif
