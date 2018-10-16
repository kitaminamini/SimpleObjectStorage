import requests
import hashlib
import random
import os

BASE_URL = 'http://127.0.0.1:8280'
STATUS_OK = requests.codes['ok']
STATUS_BAD_REQUEST = requests.codes['bad_request']
STATUS_NOT_FOUND = requests.codes['not_found']

# def test_get_status():
#     """GET /status should have status_code 200"""
#     resp = requests.get(BASE_URL + '/all')
#     assert resp.status_code == STATUS_OK

# def test_create_delete_bucket():

# 	resp = requests.post(BASE_URL + '/buckettest?create')
# 	print (resp.content)
# 	assert resp.status_code == STATUS_OK

# 	resp_delete = requests.delete(BASE_URL + '/buckettest?delete')
# 	print (resp_delete.content)
# 	assert resp_delete.status_code == STATUS_OK

# def test_get_status():
#     """GET /status should have status_code 200"""
#     resp = requests.delete(BASE_URL + '/deleteall')
#     assert resp.status_code == STATUS_OK


def test_uploading():
	resp = requests.post(BASE_URL + '/buckettest?create')
	assert resp.status_code == STATUS_OK

	resp = requests.post(BASE_URL+'/buckettest/objecttest?create')
	assert resp.status_code == STATUS_OK

	data = open('./P3.jpg', 'rb').read()
	for i in range(1,11):
		start = (1.0*(i-1))/10.0
		end = (1.0*i)/10.0
		a = (int)(len(data)*start)
		b = (int)(len(data)*end)
		D = data[a:b]
		resp = requests.put(url=BASE_URL+'/buckettest/objecttest?partNumber='+str(i),
						data=D,
						headers={'Content-Length': str(len(D)), 'Content-MD5': hashlib.md5(D).hexdigest()})
		assert resp.status_code == STATUS_OK

	resp = requests.post(BASE_URL+'/buckettest/objecttest?complete')
	assert resp.status_code == STATUS_OK

	
# def test_get_status():
#     """GET /status should have status_code 200"""
#     resp = requests.delete(BASE_URL + '/deleteall')
#     assert resp.status_code == STATUS_OK


def test_download():
	data = open('./P3.jpg', 'rb').read()
	for i in range(100):
		# print("===========================")
		# print("i = "+str(i))
		A = random.randint(0, len(data))
		B = random.randint(0, len(data))
		if (A < B):
			headers = {'Range': 'bytes='+str(A)+'-'+str(B)}
			realData = data[A:B+1]
			realMD5 = hashlib.md5(realData).hexdigest()
			# print(str(A)+"-"+str(B))
			
		else:
			headers = {'Range': 'bytes='+str(B)+'-'+str(A)}
			realData = data[B:A+1]
			realMD5 = hashlib.md5(realData).hexdigest()
			# print(str(B)+"-"+str(A))
		# print(headers)
		# print("real len: "+str(len(realData)))
		resp = requests.get(BASE_URL + '/buckettest/objecttest', headers=headers, stream=True)
		assert resp.status_code == STATUS_OK

		with open('./myOutput', 'wb') as handle:
			for block in resp.iter_content(2048):
				handle.write(block)

		downloaded = open('./myOutput', 'rb').read()
		# print("downloaded len: "+str(len(downloaded)))
		MD5 = hashlib.md5(downloaded).hexdigest()
		assert MD5 == realMD5
		os.remove('./myOutput')
		# print("===========================")

def test_full_download():
	data = open('./P3.jpg', 'rb').read()
	# print("===========================")
	# print("i = "+str(i))
	A = 0
	B = len(data)
	headers = {'Range': 'bytes='+str(A)+'-'}
	realMD5 = hashlib.md5(data).hexdigest()
		# print(str(B)+"-"+str(A))
	# print(headers)
	# print("real len: "+str(len(realData)))
	resp = requests.get(BASE_URL + '/buckettest/objecttest', headers=headers, stream=True)
	assert resp.status_code == STATUS_OK

	with open('./myOutput', 'wb') as handle:
		for block in resp.iter_content(2048):
			handle.write(block)


	downloaded = open('./myOutput', 'rb').read()
	# print("downloaded len: "+str(len(downloaded)))
	MD5 = hashlib.md5(downloaded).hexdigest()
	assert MD5 == realMD5
	os.remove('./myOutput')

	headers = {'Range': 'bytes='+str(A)+'-'+str(B)}
	resp = requests.get(BASE_URL + '/buckettest/objecttest', headers=headers, stream=True)
	assert resp.status_code == STATUS_BAD_REQUEST

	# with open('./myOutput', 'wb') as handle:
	# 	for block in resp.iter_content(2048):
	# 		handle.write(block)

	# downloaded = open('./myOutput', 'rb').read()
	# # print("downloaded len: "+str(len(downloaded)))
	# MD5 = hashlib.md5(downloaded).hexdigest()
	# assert MD5 == realMD5
	# os.remove('./myOutput')


def test_delete_bucket():
	resp = requests.post(BASE_URL + '/.a?create')
	assert resp.status_code == STATUS_BAD_REQUEST

	resp = requests.post(BASE_URL + '/toDelete?create')
	assert resp.status_code == STATUS_OK

	resp = requests.post(BASE_URL + '/toDelete?create')
	assert resp.status_code == STATUS_BAD_REQUEST

	resp = requests.post(BASE_URL+'/buckettest/objecttest?create')
	assert resp.status_code == STATUS_BAD_REQUEST

	resp = requests.post(BASE_URL + '/toDelte/some?create')
	assert resp.status_code == STATUS_BAD_REQUEST

	resp = requests.post(BASE_URL + '/toDelete/some?create')
	assert resp.status_code == STATUS_OK

	resp = requests.delete(BASE_URL + '/toDelete?delete')
	assert resp.status_code == STATUS_OK

	resp = requests.get(BASE_URL + '/toDelete?list')
	assert resp.status_code == STATUS_BAD_REQUEST

	resp = requests.post(BASE_URL + '/toDelete/some?create')
	assert resp.status_code == STATUS_BAD_REQUEST


def test_delete_object():
	resp = requests.post(BASE_URL + '/toDelete?create')
	assert resp.status_code == STATUS_OK

	resp = requests.post(BASE_URL + '/toDelete/some?create')
	assert resp.status_code == STATUS_OK

	resp = requests.post(BASE_URL + '/toDelete/some?create')
	assert resp.status_code == STATUS_BAD_REQUEST

	resp = requests.delete(BASE_URL + '/toDelete/some?delete')
	assert resp.status_code == STATUS_OK

	resp = requests.post(BASE_URL + '/toDelete/some?create')
	assert resp.status_code == STATUS_OK

	resp = requests.delete(BASE_URL + '/toDelete?delete')
	assert resp.status_code == STATUS_OK


def test_metaData():
	resp = requests.post(BASE_URL + '/forMeta?create')
	assert resp.status_code == STATUS_OK

	resp = requests.post(BASE_URL + '/forMeta/WTF?create')
	assert resp.status_code == STATUS_OK
		# PUT /{bucketName}/{objectName}?metadata&key={key}
	resp = requests.put(BASE_URL + '/forMeta/WTF?metadata&key=KK', data="insert me")
	assert resp.status_code == STATUS_OK

	resp = requests.put(BASE_URL + '/forMeta/WTF?metadata&key=KK', data="modified")
	assert resp.status_code == STATUS_OK

	resp = requests.get(BASE_URL + '/forMeta/WTF?metadata&key=KK')
	assert resp.status_code == STATUS_OK

	esp = requests.delete(BASE_URL + '/forMeta/WTF?metadata&key=KK')
	assert resp.status_code == STATUS_OK

	resp = requests.delete(BASE_URL + '/forMeta?delete')
	assert resp.status_code == STATUS_OK


def test_last():
	resp = requests.delete(BASE_URL + '/buckettest?delete')
	assert resp.status_code == STATUS_OK

	resp = requests.get(BASE_URL + '/buckettest?list')
	assert resp.status_code == STATUS_BAD_REQUEST














