from pymemcache.client import base

def main():
    # Connect to the Memcached server using the text protocol
    client = base.Client(('localhost', 11211), encoding='utf-8')

    # Set a key-value pair
    client.set('key', 'initial_value')
    print('Set operation: "key" -> "initial_value"')

    # Get the value for the key
    value = client.get('key')
    print(f'Get operation: The value for "key" is: {value}')

    # Replace the value for the existing key
    client.replace('key', 'replaced_value')
    value = client.get('key')
    print(f'Replace operation: The value for "key" is now: {value}')

    # Add a new key-value pair (will fail if the key already exists)
    add_success = client.add('new_key', 'new_value')
    if add_success:
        print('Add operation: "new_key" -> "new_value" was added.')
    else:
        print('Add operation: "new_key" already exists, could not add.')

    # Get the value for the new key
    value = client.get('new_key')
    print(f'Get operation: The value for "new_key" is: {value}')

    # Increment a counter key
    client.set('counter', 1)
    client.incr('counter', 2)
    counter_value = client.get('counter')
    print(f'Increment operation: The value for "counter" is now: {counter_value}')

    # Decrement a counter key
    client.decr('counter', 1)
    counter_value = client.get('counter')
    print(f'Decrement operation: The value for "counter" is now: {counter_value}')

    # Append to a key's value
    client.set('append_key', 'Hello')
    client.append('append_key', ' World')
    append_value = client.get('append_key')
    print(f'Append operation: The value for "append_key" is now: {append_value}')

    # Prepend to a key's value
    client.set('prepend_key', 'World')
    client.prepend('prepend_key', 'Hello ')
    prepend_value = client.get('prepend_key')
    print(f'Prepend operation: The value for "prepend_key" is now: {prepend_value}')

    # Delete a key-value pair
    client.delete('key')
    value = client.get('key')
    print(f'Delete operation: The value for "key" after deletion is: {value}')

    # Close the connection
    client.close()

if __name__ == "__main__":
    main()
