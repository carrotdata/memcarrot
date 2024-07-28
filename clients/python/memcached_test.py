from pymemcache.client import base


def main():
    # Connect to the Memcached server
    client = base.Client(('localhost', 11211))

    # Set a key-value pair
    client.set('key', 'value')

    # Get the value for the key
    value = client.get('key')
    print(f'The value for "key" is: {value.decode("utf-8")}')

    # Close the connection
    client.close()


if __name__ == "__main__":
    main()
